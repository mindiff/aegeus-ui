package io.aegeus.ipfs.portal;

import static io.nessus.ipfs.portal.NessusWebUIConstants.ENV_NESSUS_WEBUI_LABEL;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.Network;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.jaxrs.JAXRSClient;
import io.nessus.ipfs.jaxrs.SFHandle;
import io.nessus.utils.StreamUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.util.Headers;

class AegeusContentHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AegeusContentHandler.class);

    final Blockchain blockchain;
    final Network network;
    final Wallet wallet;

    final JAXRSClient client;
    final VelocityEngine ve;
    final URI gatewayURI;

    AegeusContentHandler(JAXRSClient client, Blockchain blockchain, URI gatewayURI) {
        this.blockchain = blockchain;
        this.gatewayURI = gatewayURI;
        this.client = client;

        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();

        ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {

        ByteBuffer content = null;
        try {
            String path = exchange.getRelativePath();
            if (path.startsWith("/portal")) {
                content = pageContent(exchange);
            } else {
                content = staticContent(exchange);
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            content = ByteBuffer.wrap(sw.toString().getBytes());
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/plain");
            LOG.error("Error in: " + exchange.getRequestURI(), ex);
        }

        if (content != null) {
            exchange.getResponseSender().send(content);
        }
    }

    private ByteBuffer pageContent(HttpServerExchange exchange) throws Exception {

        String relPath = exchange.getRelativePath();

        String tmplPath = null;
        VelocityContext context = new VelocityContext();
        long walletVersion = network.getNetworkInfo().version();
        long protocolVersion = network.getNetworkInfo().protocolVersion();
        context.put("walletVersion", walletVersion);
        context.put("protocolVersion", protocolVersion);
        context.put("blockCount", network.getBlockCount());

        // Action add text

        if (relPath.startsWith("/portal/addtxt")) {

            actAddText(exchange, context);
        }

        // Action add URL

        else if (relPath.startsWith("/portal/addurl")) {

            actAddURL(exchange, context);
        }

        // Action assign Label

        else if (relPath.startsWith("/portal/assign")) {

            actAssignLabel(exchange, context);
        }

        // Action file delete

        else if (relPath.startsWith("/portal/fdel")) {

            actRemoveLocalContent(exchange, context);
        }

        // Action file get

        else if (relPath.startsWith("/portal/fget")) {

            actFileGet(exchange, context);
        }

        // Action file show

        else if (relPath.startsWith("/portal/fshow")) {

            return actFileShow(exchange, context);
        }

        // Action new address

        else if (relPath.startsWith("/portal/newaddr")) {

            actNewAddress(exchange, context);
        }

        // Action import privkey

        else if (relPath.startsWith("/portal/impkey")) {

            actImportKey(exchange, context);
        }

        // Action register address

        else if (relPath.startsWith("/portal/regaddr")) {

            actRegisterAddress(exchange, context);
        }

        // Action send IPFS file

        else if (relPath.startsWith("/portal/sendcid")) {

            actSend(exchange, context);
        }

        // Page file add

        else if (relPath.startsWith("/portal/padd")) {

            tmplPath = pageFileAdd(exchange, context);
        }

        // Page file list

        else if (relPath.startsWith("/portal/files")) {

            tmplPath = pageFileList(exchange, context);
        }

        // Page file send

        else if (relPath.startsWith("/portal/psend")) {

            tmplPath = pageSend(exchange, context);
        }

        // Address List

        else if (relPath.startsWith("/portal/addresses")) {
            tmplPath = pageAddressList(exchange, context);
        }

        else if ( relPath.startsWith("/portal/balance") ) {
            tmplPath = pageGetBalance(exchange, context);
        }

	// Help section
        else if ( relPath.startsWith("/portal/help") ) {
            tmplPath = pageHelp();
        }

        // Home page

        else {

            tmplPath = pageHome(context);
        }

        if (tmplPath != null) {

            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html");

            try (InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(tmplPath))) {

                StringWriter strwr = new StringWriter();
                ve.evaluate(context, strwr, tmplPath, reader);

                return ByteBuffer.wrap(strwr.toString().getBytes());
            }
        }

        return null;
    }

    private void actAssignLabel(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String label = qparams.get("label").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        addr.setLabels(Arrays.asList(label));

        redirectAddressPage(exchange);
    }

    private void actAddText(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        String content = qparams.get("content").getFirst();

        client.add(rawAddr, relPath, new ByteArrayInputStream(content.getBytes()));

	new RedirectHandler("/portal/files?addr=" + rawAddr + "#pills-contact").handleRequest(exchange);
    }

    private void actAddURL(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        String url = qparams.get("url").getFirst();
        URL furl = null;
	int[] acceptableHttpCodes = new int[]{200,302,304};
	int remoteFileSize = 0;

        try {
            furl = new URL(url);

            URLConnection conn = furl.openConnection();
	    conn.connect();

	    if ( conn instanceof HttpURLConnection) {
   	        HttpURLConnection httpConnection = (HttpURLConnection) conn;
            	remoteFileSize = conn.getContentLength();
		int respCode = httpConnection.getResponseCode();
		boolean foundValidCode = Arrays.stream(acceptableHttpCodes).anyMatch(x->x == respCode);

		if (!foundValidCode) {
            	    new RedirectHandler("/portal/files?addr=" + rawAddr + "&error=Error Fetching Content (" + url + ")#pills-url").handleRequest(exchange);
		} else {
		    String binaryIdentifier = "application/octet-stream";
		    String remoteContentType = httpConnection.getContentType();
		    boolean isBinaryData = remoteContentType.equals(binaryIdentifier);

		    // We are not allowing binary data for this initial release, but will be included in the upcoming releases.
		    if (isBinaryData) {
                        new RedirectHandler("/portal/files?addr=" + rawAddr + "&error=Remote file is binary and not supported currently (" + url + ")#pills-url").handleRequest(exchange);
	  	    }

		    if (remoteFileSize >= 1000000) {
            	        new RedirectHandler("/portal/files?addr=" + rawAddr + "&error=Remote file size exceeds maximum of 1MB (" + url + ")#pills-url").handleRequest(exchange);
 		    } else {
        		client.add(rawAddr, relPath, furl.openStream());
			new RedirectHandler("/portal/files?addr=" + rawAddr + "#pills-url").handleRequest(exchange);
		    }
	        }
	    }
        } catch(NullPointerException e) {
            new RedirectHandler("/portal/files?addr=" + rawAddr + "&error=No content found at this address. (" + url + ")#pills-url").handleRequest(exchange);
        } catch(Exception e) {
            new RedirectHandler("/portal/files?addr=" + rawAddr + "&error=Unknown host. (" + url + ")#pills-url").handleRequest(exchange);
        }
    }

    private void actRegisterAddress(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();

        client.registerAddress(rawAddr);

        redirectHomePage(exchange);
    }

    private void actNewAddress(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String label = qparams.get("label").getFirst();

        wallet.newAddress(label);

        redirectAddressPage(exchange);
    }

    private void actRemoveLocalContent(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();

        client.removeLocalContent(rawAddr, relPath);

	new RedirectHandler("/portal/files?addr=" + rawAddr + "#pills-home").handleRequest(exchange);
    }

    private void actFileGet(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String relPath = qparams.get("path").getFirst();
        String rawAddr = qparams.get("addr").getFirst();
        String cid = qparams.get("cid").getFirst();


        try {
            client.get(rawAddr, cid, relPath, 10000L);
        } catch (IllegalStateException e) {
            String sContentExists = "Local content already exists";
            Pattern pContentExists = Pattern.compile(sContentExists);
            String errContentExists = "Local content already exists with that path for (" + cid + ").  Please type in a new path to save it again.";

            Matcher mContentExists = pContentExists.matcher(e.getMessage());
            if (mContentExists.find()) {
                new RedirectHandler("/portal/files?addr=" + rawAddr + "&error=" + errContentExists + "#pills-profile").handleRequest(exchange);
	    }
        }

        new RedirectHandler("/portal/files?addr=" + rawAddr + "#pills-profile").handleRequest(exchange);

    }

    private void actImportKey(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String key = qparams.get("impkey").getFirst();
        String label = qparams.get("label").getFirst();

        // Import key asynch
        new Thread(new Runnable() {
            public void run() {

                if (key.startsWith("A")) {
                    LOG.info("Importing watch only address: {}", key);
                    wallet.importAddress(key, Arrays.asList(label));
                } else if (key.startsWith("P")) {
                    LOG.info("Importing private key: P**************");
                    wallet.importPrivateKey(key, Arrays.asList(label));
                }
            }
        }).start();

        redirectHomePage(exchange);
    }

    private void actSend(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawFromAddr = qparams.get("fromaddr").getFirst();
        String rawToAddr = qparams.get("toaddr").getFirst();
        String cid = qparams.get("cid").getFirst();

        try {
            client.send(rawFromAddr, cid, rawToAddr, 10000L);
        } catch(IllegalArgumentException e) {
	    String sBadKey = "Cannot obtain encryption key";
	    Pattern pBadKey = Pattern.compile(sBadKey);
	    String errBadKey = "The person you are trying to send to has not registered their key.  Please ask them to do so in their addresses menu.";

	    Matcher mBadKey = pBadKey.matcher(e.getMessage());
	    if (mBadKey.find()) {
                new RedirectHandler("/portal/files?addr=" + rawFromAddr + "&error=" + errBadKey).handleRequest(exchange);
	    } else {
                new RedirectHandler("/portal/files?addr=" + rawFromAddr + "&error=" + e.getMessage() + "!").handleRequest(exchange);
	    }
        }

	new RedirectHandler("/portal/files?addr=" + rawFromAddr + "#pills-profile").handleRequest(exchange);
    }

    private ByteBuffer actFileShow(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();

        try (InputStream ins = client.getLocalContent(rawAddr, relPath)) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamUtils.copyStream(ins, baos);

            return ByteBuffer.wrap(baos.toByteArray());
        }
    }

    private String pageFileAdd(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        AddressDTO paddr = portalAddress(addr, true);
        context.put("addr", paddr);

        return "templates/portal-add.vm";
    }

    private String pageFileList(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();

        // Initializing String variable with null value
        String rawAddr = null;
        Address addr = null;
        String pubKey = null;
        String error = null;

        try {
            error = qparams.get("error").getFirst();
        } catch(NullPointerException e) { }

        try {
            rawAddr = qparams.get("addr").getFirst();
        } catch(NullPointerException e) {
            new RedirectHandler("/portal/addresses?error=No+address+found!").handleRequest(exchange);
        }

        try {
            addr = wallet.findAddress(rawAddr);
            pubKey = client.findAddressRegistation(rawAddr);
        } catch (IllegalStateException e) {
            new RedirectHandler("/portal/addresses?error=" + e.getMessage() + "!").handleRequest(exchange);
        }

        AddressDTO paddr = portalAddress(addr, pubKey != null);
        context.put("addr", paddr);

        List<SFHandle> fhandles = new ArrayList<>(client.findIPFSContent(rawAddr, 10000L));
        fhandles.addAll(client.findLocalContent(rawAddr));

        List<AddressDTO> addrs = new ArrayList<>();

        for (Address address : getAddressWithLabel()) {
            BigDecimal balance;
            if (!address.getLabels().isEmpty()) {
                String label = address.getLabels().get(0);
                balance = wallet.getBalance(label);
            } else {
                balance = wallet.getBalance(address);
            }
            String addrPubKey = client.findAddressRegistation(address.getAddress());
            addrs.add(new AddressDTO(address, balance, addrPubKey != null));
        }

        context.put("error", error);
        context.put("addrs", addrs);
        context.put("files", fhandles);
        context.put("gatewayUrl", gatewayURI);

        return "templates/portal-list.vm";
    }

    private String pageSend(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        String cid = qparams.get("cid").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        AddressDTO paddr = portalAddress(addr, true);

        List<AddressDTO> toaddrs = new ArrayList<>();
        for (Address aux : getAddressWithLabel()) {
            if (!addr.equals(aux)) {
                toaddrs.add(portalAddress(aux, true));
            }
        }

        context.put("gatewayUrl", gatewayURI);
        context.put("toaddrs", toaddrs);
        context.put("addr", paddr);
        context.put("file", new SFHandle(cid, rawAddr, relPath, true, true));

        return "templates/portal-send.vm";
    }

    private String pageGetBalance(HttpServerExchange exchange, VelocityContext context) throws Exception {
        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String qryAddr = null;

        qryAddr = qparams.get("addr").getFirst();

        List<AddressDTO> addrs = new ArrayList<>();

        for (Address addr : getAddressWithLabel()) {
            BigDecimal balance;
            if (!addr.getLabels().isEmpty()) {
                String label = addr.getLabels().get(0);
                balance = wallet.getBalance(label);
            } else {
                balance = wallet.getBalance(addr);
            }
            String pubKey = client.findAddressRegistation(addr.getAddress());
            addrs.add(new AddressDTO(addr, balance, pubKey != null));
        }

        context.put("addrs", addrs);
        context.put("query_address", qryAddr);
        return "templates/portal-balance.vm";
    }


    private String pageAddressList(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        List<AddressDTO> addrs = new ArrayList<>();

        String error = null;

        try {
            error = qparams.get("error").getFirst();
        } catch(NullPointerException e) { }

        for (Address addr : getAddressWithLabel()) {
            BigDecimal balance;
            if (!addr.getLabels().isEmpty()) {
                String label = addr.getLabels().get(0);
                balance = wallet.getBalance(label);
            } else {
                balance = wallet.getBalance(addr);
            }
            String pubKey = client.findAddressRegistation(addr.getAddress());
            addrs.add(new AddressDTO(addr, balance, pubKey != null));
        }

        String envLabel = System.getenv().get(ENV_NESSUS_WEBUI_LABEL);
        envLabel = envLabel != null ? envLabel : "Bob";

        context.put("error", error);
        context.put("envLabel", envLabel);
        context.put("addrs", addrs);

        return "templates/portal-addresses.vm";
    }

    private String pageHelp() throws Exception {
        return "templates/help.vm";
    }


    private String pageHome(VelocityContext context) throws Exception {

        int addrCount = 0;
        int wAddrCount = 0;
        int fileCount = 0;

        List<AddressDTO> addrs = new ArrayList<>();

        for (Address addr : getAddressWithLabel()) {
            BigDecimal balance = wallet.getBalance(addr);
            String pubKey = client.findAddressRegistation(addr.getAddress());

            addrs.add(new AddressDTO(addr, balance, pubKey != null));

            List<SFHandle> fhandles = new ArrayList<>(client.findIPFSContent(addr.getAddress(), 10000L));
            fhandles.addAll(client.findLocalContent(addr.getAddress()));
            fileCount = fileCount + fhandles.size();
            if (addr.isWatchOnly()) {
                wAddrCount++;
            } else {
                addrCount++;
            }
        }

        String envLabel = System.getenv().get(ENV_NESSUS_WEBUI_LABEL);
        envLabel = envLabel != null ? envLabel : "Bob";

        context.put("envLabel", envLabel);
        context.put("addrs", addrs);
        context.put("addrCount", addrCount);
        context.put("watchAddrCount", wAddrCount);
        context.put("fileCount", fileCount);

        return "templates/portal-home.vm";
    }

    // [TODO #12] Wallet generates unwanted addresses for default account
    // Here we filter addresses for the default account, if we already have labeled addresses
    private List<Address> getAddressWithLabel() {

        // Get the list of non-change addresses
        List<Address> addrs = wallet.getAddresses().stream()
                .filter(a -> !a.getLabels().contains(Wallet.LABEL_CHANGE))
                .collect(Collectors.toList());

        // Remove addrs that have no label
        List<Address> filtered = addrs.stream()
                .filter(a -> !a.getLabels().contains(""))
                .collect(Collectors.toList());

        return filtered.isEmpty() ? addrs : filtered;
    }

    private void redirectHomePage(HttpServerExchange exchange) throws Exception {
        new RedirectHandler("/portal").handleRequest(exchange);
    }

    private void redirectAddressPage(HttpServerExchange exchange) throws Exception {
            new RedirectHandler("/portal/addresses").handleRequest(exchange);
    }

    private void redirectFileList(HttpServerExchange exchange, String rawAddr) throws Exception {
        RedirectHandler handler = new RedirectHandler("/portal/plist?addr=" + rawAddr);
        handler.handleRequest(exchange);
    }

    private AddressDTO portalAddress(Address addr, boolean registered) throws GeneralSecurityException {
        BigDecimal balance = wallet.getBalance(addr);
        return new AddressDTO(addr, balance, registered);
    }

    private ByteBuffer staticContent(HttpServerExchange exchange) throws IOException {
        String path = exchange.getRelativePath();
        return getResource(path);
    }

    private ByteBuffer getResource(String resname) throws IOException {

        InputStream is = getClass().getResourceAsStream(resname);
        if (is == null)
            return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = new byte[256];
        int len = is.read(bytes);
        while (len > 0) {
            baos.write(bytes, 0, len);
            len = is.read(bytes);
        }
        return ByteBuffer.wrap(baos.toByteArray());
    }

    public static class AddressDTO {

        public final Address addr;
        public final BigDecimal balance;
        public final boolean registered;

        private AddressDTO(Address addr, BigDecimal balance, boolean registered) {
            this.addr = addr;
            this.registered = registered;
            this.balance = balance;
        }

        public String getLabel() {
            List<String> labels = addr.getLabels();
            return labels.size() > 0 ? labels.get(0) : "";
        }

        public String getAddress() {
            return addr.getAddress();
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public boolean isRegistered() {
            return registered;
        }

        public boolean isWatchOnly() {
            return addr.isWatchOnly();
        }

        @Override
        public String toString() {
            return String.format("[addr=%s, ro=%b, label=%s, reg=%b, bal=%.4f]", getAddress(), isWatchOnly(), getLabel(), isRegistered(), getBalance());
        }
    }

}
