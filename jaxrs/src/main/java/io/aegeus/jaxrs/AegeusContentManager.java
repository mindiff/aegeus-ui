package io.aegeus.jaxrs;

import io.nessus.Blockchain;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.impl.DefaultContentManager;

class AegeusContentManager extends DefaultContentManager {

    AegeusContentManager(IPFSClient ipfs, Blockchain blockchain) {
        super(ipfs, blockchain);
    }

    @Override
    protected HValues getHeaderValues() {
        return new HValues(Constants.AEG_PREFIX, Constants.AEG_VERSION);
    }
}