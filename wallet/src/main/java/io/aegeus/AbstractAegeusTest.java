package io.aegeus;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import io.nessus.BlockchainFactory;
import io.nessus.UTXO;
import io.nessus.Tx.TxBuilder;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.impl.DefaultContentManager;
import io.nessus.ipfs.impl.DefaultIPFSClient;
import io.nessus.testing.AbstractBlockchainTest;

public abstract class AbstractAegeusTest extends AbstractBlockchainTest {

    protected static ContentManager cntmgr;
    protected static AegeusBlockchain blockchain;
    protected static AegeusNetwork network;
    protected static AegeusWallet wallet;

    protected static Address addrBob;
    protected static Address addrMary;

    @BeforeClass
    public static void beforeAegeusTest() throws Exception {

        blockchain = (AegeusBlockchain) BlockchainFactory.getBlockchain(AegeusBlockchain.getAegeusConf(), AegeusBlockchain.class);
        network = (AegeusNetwork) blockchain.getNetwork();
        wallet = (AegeusWallet) blockchain.getWallet();

        IPFSClient ipfs = new DefaultIPFSClient();
        cntmgr = new DefaultContentManager(ipfs, blockchain);

        importAddresses(wallet, AbstractAegeusTest.class);

        addrBob = wallet.getAddress(LABEL_BOB);
        addrMary = wallet.getAddress(LABEL_MARY);
    }

    @AfterClass
    public static void afterAegeusTest() throws Exception {

        wallet.redeemChange(LABEL_BOB, addrBob);
        wallet.redeemChange(LABEL_MARY, addrMary);
    }

    protected void redeemLockedUtxos(String label, Address addr) {

        // Unlock all UTXOs
        wallet.listLockUnspent(Arrays.asList(addr)).stream().forEach(utxo -> wallet.lockUnspent(utxo, true));

        // Redeem all locked UTXOs
        List<UTXO> utxos = wallet.listUnspent(label);
        BigDecimal utxoAmount = getUTXOAmount(utxos);
        if (BigDecimal.ZERO.compareTo(utxoAmount) < 0) {
            BigDecimal amount = subtractFee(utxoAmount);
            wallet.sendTx(new TxBuilder()
                    .unspentInputs(utxos)
                    .output(addr.getAddress(), amount)
                    .build());
        }
    }
}
