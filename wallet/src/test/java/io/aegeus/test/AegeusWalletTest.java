package io.aegeus.test;

/*-
 * #%L
 * Aegeus :: Wallet
 * %%
 * Copyright (C) 2018 Aegeus
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.aegeus.AbstractAegeusTest;
import io.nessus.Tx;
import io.nessus.Tx.TxBuilder;
import io.nessus.TxOutput;
import io.nessus.UTXO;
import io.nessus.Wallet.Address;
import wf.bitcoin.krotjson.HexCoder;

public class AegeusWalletTest extends AbstractAegeusTest {

    @Test
    public void testRawTx () throws Exception {

        showAccountBalances();

        // Verify that Bob has some funds
        BigDecimal balBob = wallet.getBalance(addrBob);
        Assert.assertTrue(BigDecimal.ZERO.compareTo(balBob) < 0);

        BigDecimal dustAmount = network.getDustThreshold();
        BigDecimal dataAmount = dustAmount.multiply(BigDecimal.TEN);
        BigDecimal dataFee = network.getMinDataAmount();
        BigDecimal spendAmount = dataAmount.add(dataFee);

        List<UTXO> utxos = wallet.selectUnspent(LABEL_BOB, addFee(spendAmount));
        BigDecimal utxosAmount = getUTXOAmount(utxos);

        Address changeAddr = wallet.getChangeAddress(LABEL_BOB);
        BigDecimal changeAmount = utxosAmount.subtract(addFee(spendAmount));

        byte[] dataIn = "IPFS".getBytes();

        TxBuilder builder = new TxBuilder().unspentInputs(utxos);
        if (dustAmount.compareTo(changeAmount) < 0) {
            builder.output(new TxOutput(changeAddr.getAddress(), changeAmount));
        }
        Tx tx = builder
                .output(new TxOutput(addrBob.getAddress(), dataAmount, dataIn))
                .build();

        String txId = wallet.sendTx(tx);

        LOG.info("Tx: {}", txId);

        // Show account balances
        showAccountBalances();

        // Verify that OP_RETURN data has been recorded
        tx = wallet.getTransaction(txId);
        List<TxOutput> outputs = tx.outputs();
        TxOutput lastOut = outputs.get(outputs.size() - 1);
        if (dustAmount.compareTo(changeAmount) < 0) {
            Assert.assertEquals(3, outputs.size());
            Assert.assertEquals(changeAddr.getAddress(), outputs.get(0).getAddress());
            Assert.assertEquals(addrBob.getAddress(), outputs.get(1).getAddress());
        } else {
            Assert.assertEquals(2, outputs.size());
            Assert.assertEquals(addrBob.getAddress(), outputs.get(0).getAddress());
        }
        Assert.assertNotNull(lastOut.getData());
        byte[] dataOut = lastOut.getData();
        Assert.assertEquals("Expected OP_RETURN", 0x6A, dataOut[0]);
        Assert.assertEquals(dataIn.length + 2, dataOut.length);
        Assert.assertArrayEquals(dataIn, Arrays.copyOfRange(dataOut, 2, dataOut.length));
    }

    @Test
    public void testAmount () throws Exception {

        String hex = wallet.toSatoshiHex(new BigDecimal("0.01"));
        Assert.assertEquals("0x00000000000f4240", hex);

        BigDecimal val = wallet.fromSatoshiHex(hex);
        Assert.assertEquals(new BigDecimal("0.01"), val);

        byte[] bytes = HexCoder.decode(hex.substring(2));
        Assert.assertEquals(8, bytes.length);

        hex = HexCoder.encode(reverse(bytes));
        Assert.assertEquals("40420f0000000000", hex);
    }

    private byte[] reverse(byte[] bytes) {
        byte[] copy = bytes.clone();
        for (int i = 0; i < bytes.length; i++) {
            copy[bytes.length - i - 1] = bytes[i];
        }
        return copy;
    }
}
