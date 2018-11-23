package io.aegeus.test.jaxrs;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import io.aegeus.AbstractAegeusTest;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.core.ipfs.ContentManager;
import io.nessus.core.ipfs.IPFSClient;
import io.nessus.core.ipfs.impl.DefaultIPFSClient;
import io.nessus.core.ipfs.impl.ExtendedContentManager;

public abstract class AbstractJAXRSTest extends AbstractAegeusTest {

    protected static ContentManager cntmgr;

    @BeforeClass
    public static void beforeClass() throws Exception {

        AbstractAegeusTest.beforeClass();

        IPFSClient ipfs = new DefaultIPFSClient();
        cntmgr = new ExtendedContentManager(ipfs, blockchain);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        wallet.redeemChange(LABEL_BOB, addrBob);
        wallet.redeemChange(LABEL_MARY, addrMary);
    }

    protected void redeemLockedUtxos(String label, Address addr) {

        // Unlock all UTXOs
        wallet.listLockUnspent(Arrays.asList(addr))
            .forEach(utxo -> wallet.lockUnspent(utxo, true));

        // Redeem all locked UTXOs
        List<UTXO> utxos = wallet.listUnspent(label);
        String changeAddr = wallet.getChangeAddress(label).getAddress();
        wallet.sendToAddress(addr.getAddress(), changeAddr, Wallet.ALL_FUNDS, utxos);
    }
}
