/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.pantheon.ethereum.permissioning;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;

public class WhitelistPersistor {

  private File configurationFile;

  public enum WHITELIST_TYPE {
    ACCOUNTS("accounts-whitelist"),
    NODES("nodes-whitelist");

    private String tomlKey;

    WHITELIST_TYPE(final String tomlKey) {
      this.tomlKey = tomlKey;
    }

    public String getTomlKey() {
      return tomlKey;
    }
  }

  public WhitelistPersistor(final String configurationFile) {
    this.configurationFile = new File(configurationFile);
  }

  public synchronized void updateConfig(
      final WHITELIST_TYPE whitelistType, final Collection<String> updatedWhitelistValues)
      throws IOException {
    removeExistingConfigItem(whitelistType);
    addNewConfigItem(whitelistType, updatedWhitelistValues);
  }

  @VisibleForTesting
  public void removeExistingConfigItem(final WHITELIST_TYPE whitelistType) throws IOException {
    List<String> otherConfigItems;
    try (Stream<String> configKeys = Files.lines(configurationFile.toPath())) {
      otherConfigItems =
          configKeys
              .filter(line -> !line.contains(whitelistType.getTomlKey()))
              .collect(Collectors.toList());
    }

    Files.write(
        configurationFile.toPath(),
        otherConfigItems,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  @VisibleForTesting
  public void addNewConfigItem(
      final WHITELIST_TYPE whitelistType, final Collection<String> whitelistValues)
      throws IOException {
    String newConfigItem =
        String.format(
            "%s=[%s]",
            whitelistType.getTomlKey(),
            whitelistValues
                .parallelStream()
                .map(uri -> String.format("\"%s\"", uri))
                .collect(Collectors.joining(",")));

    Files.write(
        configurationFile.toPath(),
        newConfigItem.getBytes(Charsets.UTF_8),
        StandardOpenOption.WRITE,
        StandardOpenOption.APPEND);
  }
}
