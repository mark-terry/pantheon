/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.p2p.permissioning;

import tech.pegasys.pantheon.ethereum.p2p.peers.DefaultPeer;
import tech.pegasys.pantheon.ethereum.p2p.peers.Peer;
import tech.pegasys.pantheon.ethereum.permissioning.PermissioningConfiguration;
import tech.pegasys.pantheon.ethereum.permissioning.WhitelistPersistor;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;

public class NodeWhitelistController {

  private final List<Peer> nodesWhitelist = new ArrayList<>();
  private boolean nodeWhitelistSet = false;
  private final WhitelistPersistor whitelistPersistor;
  private final String WHITELIST_FAIL_MESSAGE = "Unable to update whitelist configuration file.";

  public NodeWhitelistController(final PermissioningConfiguration configuration) {
    this.whitelistPersistor = new WhitelistPersistor(configuration.getConfigurationFilePath());
    if (configuration.isNodeWhitelistSet() && configuration.getNodeWhitelist() != null) {
      for (URI uri : configuration.getNodeWhitelist()) {
        nodesWhitelist.add(DefaultPeer.fromURI(uri));
      }
      nodeWhitelistSet = true;
    }
  }

  public boolean addNode(final Peer node) {
    nodeWhitelistSet = true;
    return nodesWhitelist.add(node);
  }

  private boolean removeNode(final Peer node) {
    return nodesWhitelist.remove(node);
  }

  public NodesWhitelistResult addNodes(final List<DefaultPeer> peers) {
    if (peerListHasDuplicates(peers)) {
      return new NodesWhitelistResult(
          NodesWhitelistResultType.ERROR_DUPLICATED_ENTRY,
          String.format("Specified peer list contains duplicates"));
    }

    for (DefaultPeer peer : peers) {
      if (nodesWhitelist.contains(peer)) {
        return new NodesWhitelistResult(
            NodesWhitelistResultType.ERROR_EXISTING_ENTRY,
            String.format("Specified peer: %s already exists in whitelist.", peer.getId()));
      }
    }

    final boolean wasNodeWhitelistSet = this.nodeWhitelistSet;
    final List<Peer> oldWhitelist = new ArrayList<>(this.nodesWhitelist);

    peers.forEach(this::addNode);
    try {
      updateConfigurationFile(peerToEnodeURI(nodesWhitelist));
    } catch (IOException e) {
      revertState(wasNodeWhitelistSet, oldWhitelist);
      return new NodesWhitelistResult(
          NodesWhitelistResultType.ERROR_WHITELIST_PERSIST_FAIL, WHITELIST_FAIL_MESSAGE);
    }
    return new NodesWhitelistResult(NodesWhitelistResultType.SUCCESS);
  }

  private boolean peerListHasDuplicates(final List<DefaultPeer> peers) {
    return !peers.stream().allMatch(new HashSet<>()::add);
  }

  public NodesWhitelistResult removeNodes(final List<DefaultPeer> peers) {
    if (peerListHasDuplicates(peers)) {
      return new NodesWhitelistResult(
          NodesWhitelistResultType.ERROR_DUPLICATED_ENTRY,
          String.format("Specified peer list contains duplicates"));
    }

    for (DefaultPeer peer : peers) {
      if (!(nodesWhitelist.contains(peer))) {
        return new NodesWhitelistResult(
            NodesWhitelistResultType.ERROR_ABSENT_ENTRY,
            String.format("Specified peer: %s does not exist in whitelist.", peer.getId()));
      }
    }

    final List<Peer> oldWhitelist = new ArrayList<>(this.nodesWhitelist);

    peers.forEach(this::removeNode);
    try {
      updateConfigurationFile(peerToEnodeURI(nodesWhitelist));
    } catch (IOException e) {
      revertState(oldWhitelist);
      return new NodesWhitelistResult(
          NodesWhitelistResultType.ERROR_WHITELIST_PERSIST_FAIL, WHITELIST_FAIL_MESSAGE);
    }
    return new NodesWhitelistResult(NodesWhitelistResultType.SUCCESS);
  }

  private void updateConfigurationFile(final Collection<String> nodes) throws IOException {
    whitelistPersistor.updateConfig(WhitelistPersistor.WHITELIST_TYPE.NODES, nodes);
  }

  private void revertState(final List<Peer> nodesWhitelist) {
    this.nodesWhitelist = nodesWhitelist;
  }

  private void revertState(final boolean whitelistSet, final List<Peer> nodesWhitelist) {
    this.nodeWhitelistSet = whitelistSet;
    revertState(nodesWhitelist);
  }

  private Collection<String> peerToEnodeURI(final Collection<Peer> peers) {
    return peers.parallelStream().map(Peer::getEnodeURI).collect(Collectors.toList());
  }

  public boolean isPermitted(final Peer node) {
    return (!nodeWhitelistSet || (nodeWhitelistSet && nodesWhitelist.contains(node)));
  }

  public List<Peer> getNodesWhitelist() {
    return nodesWhitelist;
  }

  public boolean nodeWhitelistSet() {
    return nodeWhitelistSet;
  }

  public static class NodesWhitelistResult {
    private final NodesWhitelistResultType result;
    private final Optional<String> message;

    NodesWhitelistResult(final NodesWhitelistResultType fail, final String message) {
      this.result = fail;
      this.message = Optional.of(message);
    }

    @VisibleForTesting
    public NodesWhitelistResult(final NodesWhitelistResultType result) {
      this.result = result;
      this.message = Optional.empty();
    }

    public NodesWhitelistResultType result() {
      return result;
    }

    public Optional<String> message() {
      return message;
    }
  }

  public boolean contains(final Peer node) {
    return (!nodeWhitelistSet || (nodesWhitelist.contains(node)));
  }

  public enum NodesWhitelistResultType {
    SUCCESS,
    ERROR_DUPLICATED_ENTRY,
    ERROR_EXISTING_ENTRY,
    ERROR_ABSENT_ENTRY,
    ERROR_WHITELIST_PERSIST_FAIL
  }
}
