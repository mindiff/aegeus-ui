package io.aegeus;

/*-
 * #%L
 * Aegeus :: Wallet
 * %%
 * Copyright (C) 2018 Aegeus
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.AbstractBlockchain;
import io.nessus.AbstractNetwork;
import io.nessus.AbstractWallet;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class AegeusBlockchain extends AbstractBlockchain {

    static final Logger LOG = LoggerFactory.getLogger(AegeusBlockchain.class);

    public AegeusBlockchain(BitcoindRpcClient client) {
        super(client);
    }

    @Override
    protected AbstractWallet createWallet() {
        return new AegeusWallet(this, getRpcClient());
    }

    @Override
    protected AbstractNetwork createNetwork() {
        return new AegeusNetwork(this, getRpcClient());
    }

    public static Properties getAegeusConf() throws IOException {

        Properties props = new Properties();

        Path configPath = Paths.get(System.getProperty("user.home"), ".aegeus", "aegeus.conf");
        if (!configPath.toFile().isFile()) {
            LOG.warn("Cannot find aegeus.conf at: {}", configPath);
            return props;
        }

        try (FileReader fr = new FileReader(configPath.toFile())) {
            props.load(fr);
        }

        return props;
    }
}
