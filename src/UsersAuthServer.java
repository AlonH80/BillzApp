import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class UsersAuthServer extends Observable {

    private HttpsServer server;
    private Logger logger;
    private LinkedTreeMap<String, HttpExchange> pendingManagerResponse;
    private static final String resourcesPath = "resources/";

    public UsersAuthServer(int port) throws Exception {
        super();
        pendingManagerResponse = new LinkedTreeMap<>();
        SSLContext sslContext = loadCertificate();
        server = HttpsServer.create(new InetSocketAddress("localhost", port), 0);
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
        char[] password = "UsersAuth".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream("resources/ua.keystore"); //TODO: add to config file
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

    public void sendManagerRepsonse(String pendQueueId, String managerResponse) throws IOException {
        HttpExchange httpExchange = pendingManagerResponse.get(pendQueueId);
        sendDefaultResponse(httpExchange, managerResponse);
        pendingManagerResponse.remove(pendQueueId);
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
            String requestURI = httpExchange.getRequestURI().toString();
            String requestFile = "loginPage";
            String[] splitURI = requestURI.split("/");
            if (splitURI.length > 1) {
                requestFile = splitURI[1];
            }

            if (requestFile.contains("favicon")) {
                return "";
            }
            else if (!requestFile.contains(".js")) {
                requestFile = String.format("%s.html", requestFile);
            }
            requestFile = String.format("%s/%s", resourcesPath,  requestFile);
            return fileToString(requestFile);
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
                htmlContent = fileToString(String.format("%s/index.html", resourcesPath));
            }
            sendDefaultResponse(httpExchange, htmlContent);
        }

        synchronized private void handlePostResponse(HttpExchange httpExchange, String requestParamValue)  throws IOException {
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            HashMap<String, Object> userMap = (HashMap<String, Object>) Utils.jsonToMap(requestParamValue);
            Integer pendSize = pendingManagerResponse.size();
            userMap.put("pendQueueId", pendSize.toString());
            pendingManagerResponse.put(pendSize.toString(), httpExchange);
            setChanged();
            notifyObservers(userMap);
        }
    }
}

