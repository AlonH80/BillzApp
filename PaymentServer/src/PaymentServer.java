import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

public class PaymentServer extends Observable {
    private HttpsServer server;
    private Logger logger;

    public PaymentServer() throws Exception {
        super();
        SSLContext sslContext = loadCertificate();
        server = HttpsServer.create(new InetSocketAddress("localhost", 8001), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            public void configure(HttpsParameters params) {
                try {
                    // Initialise the SSL context
                    SSLContext c = SSLContext.getDefault();
                    SSLEngine engine = c.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    // Get the default parameters
                    SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);
                } catch (Exception ex) {
                    logger.info("Failed to create HTTPS port");
                }
            }
        });
        server.createContext("/", new  MyHttpHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
    }

    public void setLogger(Logger logger){
        this.logger = logger;
    }

    private SSLContext loadCertificate() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Initialise the keystore
        char[] password = "PaymentServer".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream("/Users/alonhartanu/.keychain/ps.keystore");
        ks.load(fis, password);

        // Set up the key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        // Set up the trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        // Set up the HTTPS context and parameters
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

    private class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParamValue=null;
            if("GET".equals(httpExchange.getRequestMethod())) {
                requestParamValue = handleGetRequest(httpExchange);

            }else if("POST".equals(httpExchange.getRequestMethod())) {
                requestParamValue = handlePostRequest(httpExchange);
            }
            handleResponse(httpExchange,requestParamValue);
        }

        private String handleGetRequest(HttpExchange httpExchange) throws IOException{
            HashMap<String, String> custMap = new HashMap<>(4);
            String requestURI = httpExchange.getRequestURI().toString();
            String quer;
            try {
                quer = requestURI.split("\\?")[1];
                String[] querParams = quer.split("&");
                Arrays.stream(querParams).forEach(par -> custMap.put(par.split("=")[0], par.split("=")[1]));
                logger.info(custMap.toString());
                setChanged();
                notifyObservers(custMap);
            }
            catch (Exception e){
                quer = "";
            }
            return quer;
        }

        private String handlePostRequest(HttpExchange httpExchange) throws IOException {
            InputStream bod = httpExchange.getRequestBody();
            BufferedReader br = new BufferedReader(new InputStreamReader(bod));
            StringBuilder reqBody = new StringBuilder();
            String line = br.readLine();
            while(line != null){
                reqBody.append(line);
                line = br.readLine();
            }
            logger.info(reqBody.toString());
            return reqBody.toString();
        }

        private void handleResponse(HttpExchange httpExchange, String requestParamValue)  throws IOException {
            OutputStream outputStream = httpExchange.getResponseBody();
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html>").
            append("<body>").
            append("<h1>").
            append("Hello ")
                    .append(requestParamValue)
                    .append("</h1>")
                    .append("</body>")
                    .append("</html>");
            // encode HTML content
            String htmlResponse = htmlBuilder.toString();
            // this line is a must
            httpExchange.sendResponseHeaders(200, htmlResponse.length());
            outputStream.write(htmlResponse.getBytes());
            outputStream.flush();
            outputStream.close();

        }

    }
}
