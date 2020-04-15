import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;
import com.google.gson.Gson;

public class PaymentServer extends Observable {
    private HttpsServer server;
    private Logger logger;
    private HashMap<String, HttpExchange> pendingManagerResponse;
    private static final String resourcesPath = "/Users/alonhartanu/Desktop/Java/PaymentComponent/WebResources";

    public PaymentServer() throws Exception {
        super();
        pendingManagerResponse = new HashMap<>();
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

    public void sendManagerResponse(String pendQueueId, String managerResponse) throws IOException {
        HttpExchange httpExchange = pendingManagerResponse.get(pendQueueId);
        sendDefaultResponse(httpExchange, managerResponse);
        pendingManagerResponse.remove(pendQueueId);
    }

    private void sendDefaultResponse(HttpExchange httpExchange, String response) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Access-Control-Allow-Origin", "https://localhost:63342");
        httpExchange.sendResponseHeaders(200, response.length());
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }

    public static String fileToString(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        int nextCh = br.read();
        while (nextCh != -1) {
            sb.append((char)nextCh);
            nextCh = br.read();
        }
        return sb.toString();
    }

    protected class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParamValue=null;
            try {
                if ("GET".equals(httpExchange.getRequestMethod())) {
                    requestParamValue = handleGetRequest(httpExchange);
                    handleGetResponse(httpExchange, requestParamValue);

                } else if ("POST".equals(httpExchange.getRequestMethod())) {
                    requestParamValue = handlePostRequest(httpExchange);
                    handlePostResponse(httpExchange, requestParamValue);
                }
            }
            catch (Exception e){
                logger.warning(e.getMessage());
                Arrays.stream(e.getStackTrace()).forEach(st->logger.warning(st.toString()));
            }
        }

        private String handleGetRequest(HttpExchange httpExchange) throws IOException{
            HashMap<String, String> custMap = new HashMap<>(4);
            String requestURI = httpExchange.getRequestURI().toString();
            String quer;
            if (requestURI.contains("/?")) {
                quer = requestURI.split("\\?")[1];
                String[] querParams = quer.split("&");
                Arrays.stream(querParams).forEach(par -> custMap.put(par.split("=")[0], par.split("=")[1]));
                custMap.put("type", "execute");
                logger.info(custMap.toString());
                setChanged();
                notifyObservers(custMap);
                quer = "";
            }
            else if (requestURI.contains(".js")){
                String jsFile = requestURI.split("/")[1];
                String jsPath = String.format("%s/%s", resourcesPath, jsFile);
                quer = fileToString(jsPath);
            }
            else{
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

        private void handleGetResponse(HttpExchange httpExchange, String htmlContent) throws IOException {
            if (htmlContent.isEmpty()) {
                htmlContent = fileToString(String.format("%s/pay_page.html", resourcesPath));
            }
            sendDefaultResponse(httpExchange, htmlContent);
        }

        synchronized private void handlePostResponse(HttpExchange httpExchange, String requestParamValue)  throws IOException {
            HashMap<String, Object> custMap = (new Gson()).fromJson(requestParamValue, HashMap.class);
            custMap.put("type", "transferMoney");
            Integer pendSize = pendingManagerResponse.size();
            custMap.put("pendQueueId", pendSize.toString());
            pendingManagerResponse.put(pendSize.toString(), httpExchange);
            setChanged();
            notifyObservers(custMap);
        }
    }
}
