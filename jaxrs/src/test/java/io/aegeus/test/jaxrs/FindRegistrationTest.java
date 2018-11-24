package io.aegeus.test.jaxrs;

import java.security.PublicKey;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Wallet.Address;

public class FindRegistrationTest extends AbstractJAXRSTest {

    @Test
    public void findRegistration() throws Exception {

        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findAddressRegistation(addrBob);

        if (pubKey == null) {
            cntmgr.registerAddress(addrBob);
            pubKey = cntmgr.findAddressRegistation(addrBob);
        }

        Assert.assertNotNull(pubKey);
    }
}
