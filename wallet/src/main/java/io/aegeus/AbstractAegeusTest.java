package io.aegeus;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import io.nessus.BlockchainFactory;
import io.nessus.Wallet.Address;
import io.nessus.testing.AbstractBlockchainTest;

public abstract class AbstractAegeusTest extends AbstractBlockchainTest {

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

        importAddresses(wallet, AbstractAegeusTest.class);

        addrBob = wallet.getAddress(LABEL_BOB);
        addrMary = wallet.getAddress(LABEL_MARY);
    }

    @AfterClass
    public static void afterAegeusTest() throws Exception {

        redeemChange(LABEL_BOB, addrBob);
        redeemChange(LABEL_MARY, addrMary);
    }

    // [TODO] Remove workaround for #42
    // https://github.com/tdiesler/nessus/issues/42
    private static void redeemChange(String label, Address addr) {
        List<Address> addrs = wallet.getChangeAddresses(label);
        if (!addrs.isEmpty()) {
            wallet.redeemChange(label, addr);
        }
    }
}
