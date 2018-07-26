package io.aegeus.jaxrs;

import io.nessus.Blockchain;
import io.nessus.Tx;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.impl.DefaultContentManager;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

class AegeusContentManager extends DefaultContentManager {

    AegeusContentManager(IPFSClient ipfs, Blockchain blockchain) {
        super(ipfs, blockchain);
        
        // This effectively ignores all Tx before the best block
        Integer blockHeight = network.getBlockCount();
        setMinBlockHeight(blockHeight);
    }

    @Override
    protected HValues getHeaderValues() {
        return new HValues(Constants.AEG_PREFIX, Constants.AEG_VERSION);
    }

    /**
     * [TODO #13] listlockunspent may return stale data
     * 
     * https://github.com/AegeusCoin/aegeus/issues/13
     */
    @Override
    protected Tx getLockedTransaction(String txId) {
        Tx result = null;
        try {
            result = wallet.getTransaction(txId);
        } catch (BitcoinRPCException ex) {
            if (ex.getMessage().contains("Invalid or non-wallet transaction id")) {
                return null;
            }
            throw ex;
        }
        return result;
    }
}