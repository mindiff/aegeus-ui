package io.aegeus.test.jaxrs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.aegeus.AbstractAegeusTest;
import io.aegeus.jaxrs.AegeusApplication;
import io.aegeus.jaxrs.AegeusApplication.AegeusServer;
import io.aegeus.jaxrs.AegeusClient;
import io.aegeus.jaxrs.SFHandle;
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

    @Test
    public void basicWorkflow() throws Exception {

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
        Assert.assertEquals(relPath.toString(), fhandles.get(0).getPath());
        Assert.assertEquals(1, fhandles.size());
        
        InputStream reader = client.getLocalContent(addrBob.getAddress(), relPath.toString());
        BufferedReader br = new BufferedReader(new InputStreamReader(reader));
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", br.readLine());
        
        Assert.assertTrue(client.deleteLocalContent(addrBob.getAddress(), relPath.toString()));
        Assert.assertTrue(client.findLocalContent(addrBob.getAddress()).isEmpty());
        
        // Get content from IPFS
        
        String cid = fhandle.getCid();
        
        fhandle = client.get(addrBob.getAddress(), cid, relPath.toString(), 10000L);
        
        Assert.assertEquals(addrBob, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
        
        try {
            Address addrDummy = wallet.getAddress("Dummy");
            client.get(addrDummy.getAddress(), cid, relPath.toString(), 10000L);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().startsWith("Wallet does not control private key"));
        }
        
        // Send content from IPFS
        
        fhandle = client.send(addrBob.getAddress(), cid, addrMarry.getAddress(), 10000L);
        
        Assert.assertEquals(addrMarry, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());
        
        // Find IPFS content on blockchain
        
        fhandles = client.findIPFSContent(addrMarry.getAddress(), 10000L);
        List<String> cids = fhandles.stream().map(fh -> fh.getCid()).collect(Collectors.toList());
        Assert.assertTrue(cids.contains(fhandle.getCid()));
        
        // Get content from IPFS
        
        relPath = Paths.get("marry/userfile.txt");
        fhandle = client.get(addrMarry.getAddress(), fhandle.getCid(), relPath.toString(), 10000L);
        
        Assert.assertEquals(addrMarry, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
    }
}