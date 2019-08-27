/*
 * This file is part of RskJ
 * Copyright (C) 2018 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.rpc.modules.eth;

import co.rsk.config.BridgeConstants;
import co.rsk.core.ReversibleTransactionExecutor;
import co.rsk.core.RskAddress;
import co.rsk.core.bc.BlockResult;
import co.rsk.core.bc.PendingState;
import co.rsk.db.RepositoryLocator;
import co.rsk.peg.BridgeSupportFactory;
import co.rsk.rpc.ExecutionBlockRetriever;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.TestUtils;
import org.ethereum.core.*;
import org.ethereum.rpc.TypeConverter;
import org.ethereum.rpc.Web3;
import org.ethereum.vm.program.ProgramResult;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;


import static org.ethereum.crypto.HashUtil.EMPTY_TRIE_HASH;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class EthModuleTest {
    @Test
    public void callSmokeTest() {
        Web3.CallArguments args = new Web3.CallArguments();
        BlockResult blockResult = mock(BlockResult.class);
        Block block = mock(Block.class);
        ExecutionBlockRetriever retriever = mock(ExecutionBlockRetriever.class);
        when(retriever.getExecutionBlock_workaround("latest"))
                .thenReturn(blockResult);
        when(blockResult.getBlock()).thenReturn(block);

        byte[] hreturn = TypeConverter.stringToByteArray("hello");
        ProgramResult executorResult = mock(ProgramResult.class);
        when(executorResult.getHReturn())
                .thenReturn(hreturn);

        ReversibleTransactionExecutor executor = mock(ReversibleTransactionExecutor.class);
        when(executor.executeTransaction(eq(blockResult.getBlock()), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(executorResult);

        EthModule eth = new EthModule(
                null,
                anyByte(),
                null,
                null,
                executor,
                retriever,
                null,
                null,
                null,
                null,
                new BridgeSupportFactory(
                        null, null, null));

        String result = eth.call(args, "latest");
        assertThat(result, is(TypeConverter.toJsonHex(hreturn)));
    }

    @Test
    public void getCode() {
        byte[] expectedCode = new byte[] {1, 2, 3};

        TransactionPool mockTransactionPool = mock(TransactionPool.class);
        PendingState mockPendingState = mock(PendingState.class);

        doReturn(expectedCode).when(mockPendingState).getCode(any(RskAddress.class));
        doReturn(mockPendingState).when(mockTransactionPool).getPendingState();

        EthModule eth = new EthModule(
                null,
                (byte) 0,
                null,
                mockTransactionPool,
                null,
                null,
                null,
                null,
                null,
                null,
                new BridgeSupportFactory(
                        null,
                        null,
                        null
                )
        );

        String addr = eth.getCode(TestUtils.randomAddress().toHexString(), "pending");
        Assert.assertThat(Hex.decode(addr.substring("0x".length())), is(expectedCode));
    }

    String anyAddress = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private Web3.CallArguments getTransactionParameters() {
        //
        RskAddress addr1 = new RskAddress(anyAddress);
        BigInteger value = BigInteger.valueOf(0); // do not pass value
        BigInteger gasPrice = BigInteger.valueOf(8);
        BigInteger gasLimit = BigInteger.valueOf(500000); // large enough
        String data = "0xff";

        Web3.CallArguments args = new Web3.CallArguments();
        args.from = TypeConverter.toJsonHex(addr1.getBytes());
        args.to = args.from;  // same account
        args.data = data;
        args.gas = TypeConverter.toQuantityJsonHex(gasLimit);
        args.gasPrice = TypeConverter.toQuantityJsonHex(gasPrice);
        args.value = value.toString();
        // Nonce doesn't matter
        args.nonce = "0";

        return args;
    }


    @Test
    public void estimateGas() {

        TransactionPool mockTransactionPool = mock(TransactionPool.class);
        PendingState mockPendingState = mock(PendingState.class);

        doReturn(mockPendingState).when(mockTransactionPool).getPendingState();
        Blockchain blockchain = mock(Blockchain.class);
        Block block = mock(Block.class);
        doReturn(block).when(blockchain).getBestBlock();
        RskAddress coinbase = new RskAddress(anyAddress);
        doReturn(coinbase).when(block).getCoinbase();

        ReversibleTransactionExecutor executor = mock(ReversibleTransactionExecutor.class);
        ProgramResult programResult = new ProgramResult();
        programResult.addDeductedRefund(10000);
        programResult.spendGas(30000);
        doReturn(programResult).when(executor).executeTransaction(any(),any(),
                any(),any(),any(),any(),any(),any());

        EthModule eth = new EthModule(
                null,
                (byte) 0,
                blockchain,
                mockTransactionPool,
                executor ,
                null,
                null,
                null,
                null,
                null,
                new BridgeSupportFactory(
                        null,
                        null,
                        null
                )
        );

        Web3.CallArguments args = getTransactionParameters();
        String gas = eth.estimateGas(args);
        byte[] gasReturned = Hex.decode(gas.substring("0x".length()));
        Assert.assertThat(gasReturned, is(BigIntegers.asUnsignedByteArray(BigInteger.valueOf(40000))));
    }

    @Test
    public void chainId() {
        EthModule eth = new EthModule(
                mock(BridgeConstants.class),
                (byte) 33,
                mock(Blockchain.class),
                mock(TransactionPool.class),
                mock(ReversibleTransactionExecutor.class),
                mock(ExecutionBlockRetriever.class),
                mock(RepositoryLocator.class),
                mock(EthModuleSolidity.class),
                mock(EthModuleWallet.class),
                mock(EthModuleTransaction.class),
                mock(BridgeSupportFactory.class)
        );
        assertThat(eth.chainId(), is("0x21"));
    }
}
