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
package tech.pegasys.pantheon.ethereum.eth.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static tech.pegasys.pantheon.ethereum.mainnet.ValidationResult.valid;

import tech.pegasys.pantheon.crypto.SECP256K1;
import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.ExecutionContextTestFixture;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.core.TransactionTestFixture;
import tech.pegasys.pantheon.ethereum.eth.manager.EthContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthPeers;
import tech.pegasys.pantheon.ethereum.eth.sync.state.SyncState;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpec;
import tech.pegasys.pantheon.ethereum.mainnet.TransactionValidator;
import tech.pegasys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason;
import tech.pegasys.pantheon.ethereum.mainnet.ValidationResult;
import tech.pegasys.pantheon.ethereum.permissioning.TransactionSmartContractPermissioningController;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class TransactionPoolPermissioningTest {

  private static final SECP256K1.KeyPair KEY_PAIR1 = SECP256K1.KeyPair.generate();

  @Mock ProtocolSchedule<Void> protocolSchedule;
  @Mock TransactionPool.TransactionBatchAddedListener batchAddedListener;
  @Mock SyncState syncState;
  @Mock EthContext ethContext;
  @Mock EthPeers ethPeers;
  @Mock ProtocolSpec<Void> protocolSpec;
  @Mock TransactionValidator transactionValidator;
  @Mock PeerTransactionTracker peerTransactionTracker;
  @Mock PendingTransactions pendingTransactions;

  @Mock
  TransactionSmartContractPermissioningController transactionSmartContractPermissioningController;

  private final ExecutionContextTestFixture executionContext = ExecutionContextTestFixture.create();
  private final ProtocolContext<Void> protocolContext = executionContext.getProtocolContext();
  private TransactionPool transactionPool;
  private final TransactionTestFixture builder = new TransactionTestFixture();
  private Transaction transaction = builder.nonce(1).createTransaction(KEY_PAIR1);

  @Before
  public void setUp() {
    when(ethContext.getEthPeers()).thenReturn(ethPeers);
    when(protocolSchedule.getByBlockNumber(anyLong())).thenReturn(protocolSpec);
    when(protocolSpec.getTransactionValidator()).thenReturn(transactionValidator);
    when(transactionValidator.validate(any())).thenReturn(ValidationResult.valid());
    when(transactionValidator.validateForSender(any(), nullable(Account.class), anyBoolean()))
        .thenReturn(valid());
    when(syncState.isInSync(anyLong())).thenReturn(true);

    transactionPool =
        new TransactionPool(
            pendingTransactions,
            protocolSchedule,
            protocolContext,
            batchAddedListener,
            syncState,
            ethContext,
            peerTransactionTracker,
            Optional.of(transactionSmartContractPermissioningController));
  }

  @Test
  public void permittedLocalTransactionMustBeAdded() {
    when(transactionSmartContractPermissioningController.isPermitted(any())).thenReturn(true);

    final ValidationResult<TransactionInvalidReason> result =
        transactionPool.addLocalTransaction(transaction);

    assertThat(result.isValid()).isTrue();

    verify(pendingTransactions, times(1)).addLocalTransaction(any());
    verifyNoMoreInteractions(pendingTransactions);
  }

  @Test
  public void nonPermittedLocalTransactionMustBeDiscarded() {
    when(transactionSmartContractPermissioningController.isPermitted(any())).thenReturn(false);

    final ValidationResult<TransactionInvalidReason> result =
        transactionPool.addLocalTransaction(transaction);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getInvalidReason())
        .isEqualTo(TransactionInvalidReason.TRANSACTION_NOT_PERMITTED);

    verifyZeroInteractions(pendingTransactions);
  }

  @Test
  public void permittedRemoteTransactionMustBeDiscarded() {
    when(transactionSmartContractPermissioningController.isPermitted(any())).thenReturn(true);

    final Set<Transaction> transactions = Collections.singleton(transaction);

    transactionPool.addRemoteTransactions(transactions);

    verify(pendingTransactions, times(1)).addRemoteTransaction(any());
    verifyNoMoreInteractions(pendingTransactions);
  }

  @Test
  public void nonPermittedRemoteTransactionMustBeDiscarded() {
    when(transactionSmartContractPermissioningController.isPermitted(any())).thenReturn(false);

    final Set<Transaction> transactions = Collections.singleton(transaction);

    transactionPool.addRemoteTransactions(transactions);

    verifyZeroInteractions(pendingTransactions);
  }
}
