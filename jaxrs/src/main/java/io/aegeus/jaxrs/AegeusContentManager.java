package io.aegeus.jaxrs;

import io.nessus.Blockchain;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.impl.DefaultContentManager;

class AegeusContentManager extends DefaultContentManager {

    private static long timeout = 6000; // 6 sec
    private static int attempts = 100; // 10 min
    private static int threads = 12;

    AegeusContentManager(IPFSClient ipfs, Blockchain blockchain) {
        super(ipfs, blockchain, timeout, attempts, threads);
    }

    @Override
    protected FHeaderId getFHeaderId() {
        return new FHeaderId(Constants.AEG_PREFIX, Constants.AEG_VERSION);
    }
}
