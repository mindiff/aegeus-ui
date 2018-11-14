package io.aegeus.test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.aegeus.AbstractAegeusTest;
import io.aegeus.AegeusBlockchain;
import io.aegeus.AegeusNetwork;
import io.aegeus.AegeusWallet;
import io.nessus.BlockchainFactory;
import io.nessus.UTXO;
import io.nessus.Wallet.Address;

public class AccountsBalanceTest extends AbstractAegeusTest {

    @BeforeClass
    public static void beforeClass() throws Exception {

        blockchain = (AegeusBlockchain) BlockchainFactory.getBlockchain(AegeusBlockchain.getAegeusConf(), AegeusBlockchain.class);
        network = (AegeusNetwork) blockchain.getNetwork();
        wallet = (AegeusWallet) blockchain.getWallet();

    }

    @Test
    public void testBalance() throws Exception {

        // API may show incorrect balance
        // https://github.com/tdiesler/nessus/issues/40

        Address addrA = wallet.newAddress("Joe");
        BigDecimal balA = wallet.getBalance(addrA);
        Assert.assertEquals(BigDecimal.ZERO, balA);

        Address addrB = wallet.importPrivateKey("PRyMQhrufMrjotxEUZnXXTyx15jeqoz6e1qh9iyTnJtHuhLMqUv5", Arrays.asList("Joe"));
        BigDecimal balB = wallet.getBalance(addrB);
        balA = wallet.getBalance(addrA);

        LOG.info("{} => {}", addrA, balA);
        LOG.info("{} => {}", addrB, balB);

        Assert.assertEquals(BigDecimal.ZERO, balA);
        Assert.assertTrue(balA.compareTo(balB) < 0);

        // Redeem change may transfer funds to another account
        // https://github.com/tdiesler/nessus/issues/42

        List<UTXO> utxos = wallet.listUnspent(Collections.emptyList());
        utxos.forEach(utxo -> LOG.info("{}", utxo));
        Assert.assertTrue(utxos.isEmpty());
    }
}
