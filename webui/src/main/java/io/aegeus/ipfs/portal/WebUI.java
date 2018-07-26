package io.aegeus.ipfs.portal;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aegeus.AegeusBlockchain;
import io.aegeus.jaxrs.AegeusApplication;
import io.aegeus.jaxrs.AegeusClient;
import io.aegeus.jaxrs.AegeusSanityCheck;
import io.aegeus.jaxrs.Constants;
import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.ipfs.IPFSClient;
import io.undertow.Undertow;

public class WebUI {

    private static final Logger LOG = LoggerFactory.getLogger(WebUI.class);

    public static void main(String[] args) throws Exception {

        AegeusSanityCheck.verifyPlatform();
        
        try {
            
            String envHost = System.getenv(Constants.ENV_JAXRS_HOST);
            String envPort = System.getenv(Constants.ENV_JAXRS_PORT);
            envHost = envHost != null ? envHost : "127.0.0.1";
            envPort = envPort != null ? envPort : "8081";
            URI jaxrsURI = new URI(String.format("http://%s:%s/aegeus", envHost, envPort));
            LOG.info("AEG JAXRS: {}", jaxrsURI);
            
            AegeusClient client = new AegeusClient(jaxrsURI);
            
            envHost = System.getenv(IPFSClient.ENV_IPFS_GATEWAY_HOST);
            envPort = System.getenv(IPFSClient.ENV_IPFS_GATEWAY_PORT);
            envHost = envHost != null ? envHost : "127.0.0.1";
            envPort = envPort != null ? envPort : "8080";
            URI gatewayURI = new URI(String.format("http://%s:%s/ipfs", envHost, envPort));
            LOG.info("IPFS Gateway: {}", gatewayURI);
            
            envHost = System.getenv().get(Constants.ENV_WEBUI_HOST);
            envPort = System.getenv().get(Constants.ENV_WEBUI_PORT);
            envHost = envHost != null ? envHost : "0.0.0.0";
            envPort = envPort != null ? envPort : "8082";
            LOG.info("AEG WebUI: http://" + envHost + ":" + envPort + "/portal");
            
            URL rpcUrl = AegeusApplication.rpcUrl();
            Blockchain blockchain = BlockchainFactory.getBlockchain(rpcUrl, AegeusBlockchain.class);
            Network network = blockchain.getNetwork();
            String networkName = network.getClass().getSimpleName();
            LOG.info("{} Version: {}",  networkName, network.getNetworkInfo().version());
            
            Undertow server = Undertow.builder()
                    .addHttpListener(Integer.valueOf(envPort), envHost, new ContentHandler(client, blockchain, gatewayURI))
                    .build();

            server.start();
                
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
