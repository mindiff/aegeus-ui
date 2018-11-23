package io.aegeus.ipfs.portal;

import java.net.URI;

import io.nessus.Blockchain;
import io.nessus.ipfs.jaxrs.JAXRSClient;
import io.nessus.ipfs.jaxrs.JAXRSSanityCheck;
import io.nessus.ipfs.portal.NessusWebUI;
import io.undertow.server.HttpHandler;

public class AegeusWebUI extends NessusWebUI {

    public static void main(String[] args) throws Exception {

        JAXRSSanityCheck.verifyPlatform();

        AegeusWebUI webUI = new AegeusWebUI(args);
        webUI.start();

    }

    public AegeusWebUI(String[] args) {
        super (args);
    }

    @Override
    protected String getApplicationName() {
        return "Aegeus";
    }

    @Override
    protected HttpHandler createHttpHandler(URI gatewayURI, Blockchain blockchain, JAXRSClient jaxrsClient) {
        return new AegeusContentHandler(jaxrsClient, blockchain, gatewayURI);
    }

}
