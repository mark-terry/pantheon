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
package tech.pegasys.pantheon.ethereum.permissioning;

import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.io.IOException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AccountWhitelistController {

  private static final int ACCOUNT_BYTES_SIZE = 20;
  private List<String> accountWhitelist = new ArrayList<>();
  private boolean isAccountWhitelistSet = false;
  private final WhitelistPersistor whitelistPersistor;

  public AccountWhitelistController(final PermissioningConfiguration configuration) {
    this(configuration, new WhitelistPersistor(configuration.getConfigurationFilePath()));
  }

  public AccountWhitelistController(
      final PermissioningConfiguration configuration, final WhitelistPersistor whitelistPersistor) {
    this.whitelistPersistor = whitelistPersistor;
    if (configuration != null && configuration.isAccountWhitelistSet()) {
      addAccounts(configuration.getAccountWhitelist());
    }
  }

  public AddResult addAccounts(final List<String> accounts) {
    if (containsInvalidAccount(accounts)) {
      return AddResult.ERROR_INVALID_ENTRY;
    }

    if (inputHasDuplicates(accounts)) {
      return AddResult.ERROR_DUPLICATED_ENTRY;
    }

    boolean inputHasExistingAccount = accounts.stream().anyMatch(accountWhitelist::contains);
    if (inputHasExistingAccount) {
      return AddResult.ERROR_EXISTING_ENTRY;
    }

    final boolean wasAccountWhitelistSet = this.isAccountWhitelistSet;
    final List<String> oldWhitelist = new ArrayList<>(this.accountWhitelist);

    this.isAccountWhitelistSet = true;
    this.accountWhitelist.addAll(accounts);
    try {
      updateConfigurationFile(accountWhitelist);
    } catch (IOException e) {
      revertState(wasAccountWhitelistSet, oldWhitelist);
      return AddResult.ERROR_WHITELIST_PERSIST_FAIL;
    }
    return AddResult.SUCCESS;
  }

  public RemoveResult removeAccounts(final List<String> accounts) {
    if (containsInvalidAccount(accounts)) {
      return RemoveResult.ERROR_INVALID_ENTRY;
    }

    if (inputHasDuplicates(accounts)) {
      return RemoveResult.ERROR_DUPLICATED_ENTRY;
    }

    if (!accountWhitelist.containsAll(accounts)) {
      return RemoveResult.ERROR_ABSENT_ENTRY;
    }

    final List<String> oldWhitelist = new ArrayList<>(this.accountWhitelist);

    this.accountWhitelist.removeAll(accounts);

    try {
      updateConfigurationFile(accountWhitelist);
    } catch (IOException e) {
      revertState(oldWhitelist);
      return RemoveResult.ERROR_WHITELIST_PERSIST_FAIL;
    }
    return RemoveResult.SUCCESS;
  }

  private void updateConfigurationFile(final List<String> accounts) throws IOException {
    whitelistPersistor.updateConfig(WhitelistPersistor.WHITELIST_TYPE.ACCOUNTS, accounts);
  }

  private void revertState(final List<String> accountWhitelist) {
    this.accountWhitelist = accountWhitelist;
  }

  private void revertState(final boolean whitelistSet, final List<String> accountWhitelist) {
    this.isAccountWhitelistSet = whitelistSet;
    revertState(accountWhitelist);
  }

  private boolean inputHasDuplicates(final List<String> accounts) {
    return !accounts.stream().allMatch(new HashSet<>()::add);
  }

  public boolean contains(final String account) {
    return (!isAccountWhitelistSet || accountWhitelist.contains(account));
  }

  public boolean isAccountWhiteListSet() {
    return isAccountWhitelistSet;
  }

  public List<String> getAccountWhitelist() {
    return new ArrayList<>(accountWhitelist);
  }

  private boolean containsInvalidAccount(final List<String> accounts) {
    return !accounts.stream().allMatch(this::isValidAccountString);
  }

  private boolean isValidAccountString(final String account) {
    try {
      BytesValue bytesValue = BytesValue.fromHexString(account);
      return bytesValue.size() == ACCOUNT_BYTES_SIZE;
    } catch (NullPointerException | IndexOutOfBoundsException | IllegalArgumentException e) {
      return false;
    }
  }

  public enum AddResult {
    SUCCESS,
    ERROR_DUPLICATED_ENTRY,
    ERROR_EXISTING_ENTRY,
    ERROR_INVALID_ENTRY,
    ERROR_WHITELIST_PERSIST_FAIL
  }

  public enum RemoveResult {
    SUCCESS,
    ERROR_ABSENT_ENTRY,
    ERROR_INVALID_ENTRY,
    ERROR_DUPLICATED_ENTRY,
    ERROR_WHITELIST_PERSIST_FAIL
  }
}
