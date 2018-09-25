package io.aegeus.test.jaxrs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.aegeus.AbstractAegeusTest;
import io.aegeus.jaxrs.AegeusApplication;
import io.aegeus.jaxrs.AegeusApplication.AegeusServer;
import io.aegeus.jaxrs.AegeusClient;
import io.aegeus.jaxrs.SFHandle;
import io.nessus.Tx.TxBuilder;
import io.nessus.UTXO;
import io.nessus.Wallet.Address;

public class AegeusFrontendTest extends AbstractAegeusTest {
    
    static AegeusServer server;
    static AegeusClient client;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        
        server = AegeusApplication.serverStart();
        
        int port = server.getAegeusConfig().getPort();
        String host = server.getAegeusConfig().getHost();
        client = new AegeusClient(new URI(String.format("http://%s:%d/aegeus", host, port)));
    }

    @AfterClass
    public static void stop() throws Exception {
        server.stop();
    }

    @Before
    public void before() {
        
        redeemLockedUtxos(LABEL_BOB, addrBob);
        redeemLockedUtxos(LABEL_MARRY, addrMarry);
        
        // Verify that Bob has some funds
        BigDecimal balBob = wallet.getBalance(addrBob);
        Assert.assertTrue(BigDecimal.ZERO.compareTo(balBob) < 0);
        
        // Verify that Marry has some funds
        BigDecimal balMarry = wallet.getBalance(addrMarry);
        if (BigDecimal.ZERO.compareTo(balMarry) == 0) {
            BigDecimal amount = balBob.divide(new BigDecimal(2));
            wallet.sendFromLabel(LABEL_BOB, addrMarry.getAddress(), amount);
        }
    }

    @After
    public void after() {
        
        redeemLockedUtxos(LABEL_BOB, addrBob);
        redeemLockedUtxos(LABEL_MARRY, addrMarry);
    }

    @Test
    public void basicWorkflow() throws Exception {

        Long timeout = 4000L;
        
        // Register Bob's public encryption key
        
        String encKey = client.register(addrBob.getAddress());
        Assert.assertNotNull(encKey);
        
        // Find Bob's pubKey registration
        
        String wasKey = client.findRegistation(addrBob.getAddress());
        Assert.assertEquals(encKey, wasKey);
        
        // Register Marry's public encryption key
        
        encKey = client.register(addrMarry.getAddress());
        Assert.assertNotNull(encKey);
        
        // Find Marry's pubKey registration
        
        wasKey = client.findRegistation(addrMarry.getAddress());
        Assert.assertEquals(encKey, wasKey);
        
        // Add content to IPFS
        
        Path relPath = Paths.get("bob/userfile.txt");
        InputStream input = getClass().getResourceAsStream("/userfile.txt");
        
        SFHandle fhandle = client.add(addrBob.getAddress(), relPath.toString(), input);

        Assert.assertEquals(addrBob, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());
        
        // Verify local file content
        
        List<SFHandle> fhandles = client.findLocalContent(addrBob.getAddress());
        Assert.assertEquals(1, fhandles.size());
        SFHandle fhLocal = fhandles.get(0);
        Assert.assertEquals(relPath.toString(), fhLocal.getPath());
        Assert.assertTrue(fhLocal.isAvailable());
        Assert.assertFalse(fhLocal.isExpired());
        Assert.assertFalse(fhLocal.isEncrypted());
        
        InputStream reader = client.getLocalContent(addrBob.getAddress(), relPath.toString());
        BufferedReader br = new BufferedReader(new InputStreamReader(reader));
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", br.readLine());
        
        Assert.assertTrue(client.deleteLocalContent(addrBob.getAddress(), relPath.toString()));
        Assert.assertTrue(client.findLocalContent(addrBob.getAddress()).isEmpty());
        
        // Find IPFS content on blockchain
        
        String cidBob = fhandle.getCid();
        fhandle = findIPFSContent(addrBob, cidBob, timeout);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertEquals(addrBob, wallet.findAddress(fhandle.getOwner()));
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Get content from IPFS
        
        String cid = fhandle.getCid();
        
        fhandle = client.get(addrBob.getAddress(), cid, relPath.toString(), timeout);
        
        Assert.assertEquals(addrBob, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
        
        try {
            Address addrDummy = wallet.getAddress("Dummy");
            client.get(addrDummy.getAddress(), cid, relPath.toString(), timeout);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().startsWith("Wallet does not control private key"));
        }
        
        // Send content from IPFS
        
        fhandle = client.send(addrBob.getAddress(), cid, addrMarry.getAddress(), timeout);
        
        Assert.assertEquals(addrMarry, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());
        
        // Find IPFS content on blockchain
        
        String cidMarry = fhandle.getCid();
        fhandle = findIPFSContent(addrMarry, cidMarry, timeout);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertEquals(addrMarry, wallet.findAddress(fhandle.getOwner()));
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Get content from IPFS
        
        relPath = Paths.get("marry/userfile.txt");
        fhandle = client.get(addrMarry.getAddress(), fhandle.getCid(), relPath.toString(), timeout);
        
        Assert.assertEquals(addrMarry, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
    }

    private SFHandle findIPFSContent(Address addr, String cid, Long timeout) throws IOException, InterruptedException {
        
        List<SFHandle> fhandles = client.findIPFSContent(addr.getAddress(), timeout);
        SFHandle fhandle  = fhandles.stream().filter(fh -> fh.getCid().equals(cid)).findFirst().get();
        Assert.assertNotNull(fhandle);
        Assert.assertFalse(fhandle.isAvailable());
        
        for (int i = 0; i < 4 && !fhandle.isAvailable(); i++) {
            Thread.sleep(1000);
            fhandles = client.findIPFSContent(addr.getAddress(), timeout);
            fhandle  = fhandles.stream().filter(fh -> fh.getCid().equals(cid)).findFirst().get();
        }
        
        return fhandle;
    }

    private void redeemLockedUtxos(String label, Address addr) {
        
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