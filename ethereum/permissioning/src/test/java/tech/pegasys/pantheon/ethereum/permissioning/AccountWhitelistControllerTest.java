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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.ethereum.permissioning.AccountWhitelistController.AddResult;
import tech.pegasys.pantheon.ethereum.permissioning.AccountWhitelistController.RemoveResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AccountWhitelistControllerTest {

  private AccountWhitelistController controller;
  @Mock private PermissioningConfiguration permissioningConfig;
  @Mock private WhitelistPersistor whitelistPersistor;

  @Before
  public void before() {
    controller = new AccountWhitelistController(permissioningConfig, whitelistPersistor);
  }

  @Test
  public void newInstanceWithNullPermConfigShouldHaveAccountWhitelistNotSet() {
    controller = new AccountWhitelistController(null, whitelistPersistor);

    assertThat(controller.isAccountWhiteListSet()).isFalse();
  }

  @Test
  public void whenAccountWhitelistIsNotSetContainsShouldReturnTrue() {
    when(permissioningConfig.isAccountWhitelistSet()).thenReturn(false);
    controller = new AccountWhitelistController(permissioningConfig, whitelistPersistor);

    assertThat(controller.contains("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73")).isTrue();
  }

  @Test
  public void whenPermConfigHasAccountsShouldSetAccountsWhitelist() {
    when(permissioningConfig.isAccountWhitelistSet()).thenReturn(true);
    when(permissioningConfig.getAccountWhitelist())
        .thenReturn(singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));
    controller = new AccountWhitelistController(permissioningConfig, whitelistPersistor);

    assertThat(controller.isAccountWhiteListSet()).isTrue();
  }

  @Test
  public void whenPermConfigHasAccountsShouldAddAllAccountsToWhitelist() {
    when(permissioningConfig.isAccountWhitelistSet()).thenReturn(true);
    when(permissioningConfig.getAccountWhitelist())
        .thenReturn(singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));
    controller = new AccountWhitelistController(permissioningConfig, whitelistPersistor);

    assertThat(controller.getAccountWhitelist())
        .contains("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73");
  }

  @Test
  public void whenPermConfigContainsEmptyListOfAccountsContainsShouldReturnFalse() {
    when(permissioningConfig.isAccountWhitelistSet()).thenReturn(true);
    when(permissioningConfig.getAccountWhitelist()).thenReturn(new ArrayList<>());
    controller = new AccountWhitelistController(permissioningConfig, whitelistPersistor);

    assertThat(controller.contains("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73")).isFalse();
  }

  @Test
  public void addAccountsWithInvalidAccountShouldReturnInvalidEntryResult() {
    AddResult addResult = controller.addAccounts(singletonList("0x0"));

    assertThat(addResult).isEqualTo(AddResult.ERROR_INVALID_ENTRY);
    assertThat(controller.getAccountWhitelist()).isEmpty();
  }

  @Test
  public void addExistingAccountShouldReturnExistingEntryResult() {
    controller.addAccounts(singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));
    AddResult addResult =
        controller.addAccounts(singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));

    assertThat(addResult).isEqualTo(AddResult.ERROR_EXISTING_ENTRY);
    assertThat(controller.getAccountWhitelist())
        .containsExactly("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73");
  }

  @Test
  public void addValidAccountsShouldReturnSuccessResult() {
    AddResult addResult =
        controller.addAccounts(singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));

    assertThat(addResult).isEqualTo(AddResult.SUCCESS);
    assertThat(controller.getAccountWhitelist())
        .containsExactly("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73");
  }

  @Test
  public void removeExistingAccountShouldReturnSuccessResult() {
    controller.addAccounts(singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));

    RemoveResult removeResult =
        controller.removeAccounts(singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));

    assertThat(removeResult).isEqualTo(RemoveResult.SUCCESS);
    assertThat(controller.getAccountWhitelist()).isEmpty();
  }

  @Test
  public void removeAbsentAccountShouldReturnAbsentEntryResult() {
    RemoveResult removeResult =
        controller.removeAccounts(singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));

    assertThat(removeResult).isEqualTo(RemoveResult.ERROR_ABSENT_ENTRY);
    assertThat(controller.getAccountWhitelist()).isEmpty();
  }

  @Test
  public void removeInvalidAccountShouldReturnInvalidEntryResult() {
    RemoveResult removeResult = controller.removeAccounts(singletonList("0x0"));

    assertThat(removeResult).isEqualTo(RemoveResult.ERROR_INVALID_ENTRY);
    assertThat(controller.getAccountWhitelist()).isEmpty();
  }

  @Test
  public void addDuplicatedAccountShouldReturnDuplicatedEntryResult() {
    AddResult addResult =
        controller.addAccounts(
            Arrays.asList(
                "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73",
                "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));

    assertThat(addResult).isEqualTo(AddResult.ERROR_DUPLICATED_ENTRY);
  }

  @Test
  public void removeDuplicatedAccountShouldReturnDuplicatedEntryResult() {
    RemoveResult removeResult =
        controller.removeAccounts(
            Arrays.asList(
                "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73",
                "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"));

    assertThat(removeResult).isEqualTo(RemoveResult.ERROR_DUPLICATED_ENTRY);
  }

  @Test
  public void stateShouldRevertIfWhitelistPersistFails() throws IOException {
    List<String> newAccount = singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73");
    List<String> newAccount2 = singletonList("0xfe3b557e8fb62b89f4916b721be55ceb828dbd72");

    assertThat(controller.isAccountWhiteListSet()).isFalse();
    assertThat(controller.getAccountWhitelist().size()).isEqualTo(0);

    controller.addAccounts(newAccount);
    assertThat(controller.isAccountWhiteListSet()).isTrue();
    assertThat(controller.getAccountWhitelist().size()).isEqualTo(1);

    doThrow(new IOException()).when(whitelistPersistor).updateConfig(any(), any());
    controller.addAccounts(newAccount2);

    assertThat(controller.isAccountWhiteListSet()).isTrue();
    assertThat(controller.getAccountWhitelist().size()).isEqualTo(1);
    assertThat(controller.getAccountWhitelist()).isEqualTo(newAccount);

    verify(whitelistPersistor, times(2)).updateConfig(any(), any());
    verifyNoMoreInteractions(whitelistPersistor);
  }
}
