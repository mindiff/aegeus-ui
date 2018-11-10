package io.aegeus.test.jaxrs;

import java.security.PublicKey;

import org.junit.Assert;
import org.junit.Test;

import io.aegeus.AbstractAegeusTest;
import io.nessus.Wallet.Address;

public class FindRegistrationTest extends AbstractAegeusTest {

    @Test
    public void findRegistration() throws Exception {

        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findRegistation(addrBob);

        if (pubKey == null) {
            cntmgr.register(addrBob);
            pubKey = cntmgr.findRegistation(addrBob);
        }

        Assert.assertNotNull(pubKey);
    }
}
