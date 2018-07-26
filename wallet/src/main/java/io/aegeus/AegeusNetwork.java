package io.aegeus;

import java.math.BigDecimal;

import io.nessus.AbstractNetwork;
import io.nessus.Block;
import io.nessus.Blockchain;
import io.nessus.bitcoin.BitcoinBlock;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class AegeusNetwork extends AbstractNetwork {

    public AegeusNetwork(Blockchain blockchain, BitcoindRpcClient client) {
        super(blockchain, client);
    }

    @Override
    public BigDecimal estimateFee() {
        return new BigDecimal("0.001");
    }

    @Override
    public Integer getBlockRate() {
        return 60;
    }

    /**
     * Dust is a txout less than 1820 * 3 = 5460 duffs
     * 
     * @see https://github.com/AegeusCoin/aegeus/blob/master/src/primitives/transaction.h#L159
     */
    @Override
    public BigDecimal getDustThreshold() {
        return new BigDecimal("0.00005460");

    }

    /**
     * This should be zero, but we give 110% of the dust threshold
     */
    @Override
    public BigDecimal getMinDataAmount() {
        BigDecimal dust = getDustThreshold();
        return dust.multiply(new BigDecimal("1.1"));
    }

    @Override
    public Block getBlock(String blockHash) {
        return new BitcoinBlock(client.getBlock(blockHash));
    }
}
