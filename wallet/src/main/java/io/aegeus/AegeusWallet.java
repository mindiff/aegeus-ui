package io.aegeus;

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
import java.math.RoundingMode;
import java.util.List;

import io.nessus.AbstractBlockchain;
import io.nessus.AbstractWallet;
import io.nessus.Tx;
import io.nessus.Tx.TxBuilder;
import io.nessus.TxOutput;
import io.nessus.bitcoin.BitcoinAddress;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.krotjson.HexCoder;

public class AegeusWallet extends AbstractWallet {

    public static final BigDecimal MIN_DATA_FEE = new BigDecimal("0.00005500");

    public AegeusWallet(AbstractBlockchain blockchain, BitcoindRpcClient client) {
        super(blockchain, client);
    }

    /*
     * The bitcoin wallet uses address type 'legacy', which AEG does not support
     */
    @Override
    protected BitcoinAddress createNewAddress(List<String> labels) {
        String rawAddr = client.getNewAddress(concatLabels(labels));
        AssertState.assertTrue(isP2PKH(rawAddr), "Not a P2PKH address: " + rawAddr);
        return new BitcoinAddress(this, rawAddr, labels);
    }

    @Override
    public boolean isP2PKH(String addr) {
        return addr.startsWith("A") || addr.startsWith("P");
    }


    @Override
    protected Address createAdddressFromRaw(String rawAddr, List<String> labels) {
        return new BitcoinAddress(this, rawAddr, labels);
    }

    /**
     * [FIXME #13] listlockunspent may return stale data
     * https://github.com/AegeusCoin/aegeus/issues/13
     */
    @Override
    protected Tx getLockedTransaction(String txId) {
        Tx result = null;
        try {
            result = super.getTransaction(txId);
        } catch (BitcoinRPCException ex) {
            if (ex.getMessage().contains("Invalid or non-wallet transaction id")) {
                return null;
            }
            throw ex;
        }
        return result;
    }

    @Override
    public String sendTx(Tx tx) {

        List<TxOutput> outputs = tx.outputs();
        TxOutput lastOut = outputs.get(outputs.size() - 1);
        if (lastOut.getData() == null) return super.sendTx(tx);

        // Below is a hack that replaces the locking script for a payment to a dummy address
        // with an OP_RETURN data script
        //
        // We need to do this because the AEG client does not accept 'data' in createrawtransaction
        //
        // [FIXME #14] Add support for data in createrawtransaction
        // https://github.com/AegeusCoin/aegeus/issues/14

        String addr = lastOut.getAddress();
        BigDecimal amount = lastOut.getAmount();
        byte[] data = lastOut.getData();

        TxBuilder builder = new TxBuilder().inputs(tx.inputs());
        for (TxOutput out : outputs) {
            if (out != lastOut) {
                builder.output(out);
            }
        }
        builder.output(new TxOutput(addr, amount));
        tx = builder.build();

        String rawTx = createRawTx(tx, data);
        String signedTx = signRawTx(rawTx, tx.inputs());
        return sendRawTransaction(signedTx);
    }

    private String createRawTx(Tx tx, byte[] dataIn) {

        AssertArgument.assertTrue(dataIn.length <= 80, "Cannot encode more than 80 bytes of data");

        String suffix = "00000000";
        String dummyAddr = "APLrCxYCxyaKmUbypKS4YmXnrAov8kdpbt";

        BigDecimal dataAmount = getBlockchain().getNetwork().getMinDataAmount();

        tx = new TxBuilder()
                .inputs(tx.inputs())
                .outputs(tx.outputs())
                .output(new TxOutput(dummyAddr, dataAmount))
                .build();

        String rawTx = super.createRawTx(tx);

        String hexAmount = toSatoshiHex(dataAmount);
        byte[] bytes = HexCoder.decode(hexAmount.substring(2));
        hexAmount = HexCoder.encode(reverse(bytes));

        int zeroAmountIdx = rawTx.lastIndexOf(hexAmount);
        AssertState.assertNotNull(zeroAmountIdx, "Cannot find amount index: " + rawTx);
        AssertState.assertTrue(rawTx.endsWith(suffix), "Unsupported final bytes: " + rawTx);

        byte[] scriptData = new byte[dataIn.length + 3];
        scriptData[0] = (byte) (dataIn.length + 2);
        scriptData[1] = 0x6a; // OP_RETURN
        scriptData[2] = (byte) dataIn.length;
        System.arraycopy(dataIn, 0, scriptData, 3, dataIn.length);

        rawTx = rawTx.substring(0, zeroAmountIdx + 16);
        rawTx = rawTx + HexCoder.encode(scriptData);
        rawTx = rawTx + suffix;

        return rawTx;
    }

    public String toSatoshiHex(BigDecimal val) {
        Long lval = val.multiply(new BigDecimal(100000000)).longValue();
        return String.format("0x%16s", Long.toHexString(lval)).replace(' ', '0');
    }

    public BigDecimal fromSatoshiHex(String hex) {
        BigDecimal satoshi = new BigDecimal(Long.decode(hex));
        return satoshi.divide(new BigDecimal(100000000), 8, RoundingMode.UNNECESSARY).stripTrailingZeros();
    }

    private byte[] reverse(byte[] bytes) {
        byte[] copy = bytes.clone();
        for (int i = 0; i < bytes.length; i++) {
            copy[bytes.length - i - 1] = bytes[i];
        }
        return copy;
    }
}
