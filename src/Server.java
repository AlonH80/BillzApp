import com.google.gson.internal.LinkedTreeMap;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server extends Observable {
    private HttpServer server;
    private Logger logger;
    private static final String resourcesPath = "resources/"; //"/Users/alonhartanu/Desktop/Java/PaymentComponent/WebResources";
    private PaymentManager paymentManager;
    private UsersManager usersManager;
    private LinkedTreeMap<String, HttpExchange> pendingManagerResponse;

    public Server() throws Exception {
        usersManager = new UsersManager(this);
        setLogger((usersManager.getLogger()));
        paymentManager = new PaymentManager(this);
        pendingManagerResponse = new LinkedTreeMap<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
        server.createContext("/", new MyHttpHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void sendManagerResponse(String pendQueueId, String managerResponse) throws IOException {
        HttpExchange httpExchange = pendingManagerResponse.get(pendQueueId);
        sendDefaultResponse(httpExchange, managerResponse);
        pendingManagerResponse.remove(pendQueueId);
    }

    private void sendDefaultResponse(HttpExchange httpExchange, String response) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        //Headers responseHeaders = httpExchange.getResponseHeaders();
        //responseHeaders.set("Access-Control-Allow-Origin", "https://localhost:63342");
        httpExchange.sendResponseHeaders(200, response.length());
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }


    protected class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try {
                if ("GET".equals(httpExchange.getRequestMethod())) {
                    String requestParamValue = handleGetRequest(httpExchange);
                    //handleGetResponse(httpExchange, requestParamValue);

                } else if ("POST".equals(httpExchange.getRequestMethod())) {
                    HashMap<String, Object> requestParamValue = handlePostRequest(httpExchange);
                    handlePostResponse(httpExchange, requestParamValue);
                }
            } catch (Exception e) {
                logger.warning(e.getMessage());
                Arrays.stream(e.getStackTrace()).forEach(st -> logger.warning(st.toString()));
            }
        }


        private String handleGetRequest(HttpExchange httpExchange) throws IOException {
            sendDefaultResponse(httpExchange, "<p>123</p>");
            return "";
        }

        private HashMap<String, Object> handlePostRequest(HttpExchange httpExchange) throws IOException {
            InputStream bod = httpExchange.getRequestBody();
            BufferedReader br = new BufferedReader(new InputStreamReader(bod));
            StringBuilder reqBody = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                reqBody.append(line);
                line = br.readLine();
            }
            logger.info(reqBody.toString());
            return (HashMap<String, Object>) Utils.jsonToMap(reqBody.toString());
        }

        synchronized private void handlePostResponse(HttpExchange httpExchange, HashMap<String, Object> requestParamValue) throws IOException {
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            Integer pendSize = pendingManagerResponse.size();
            requestParamValue.put("pendQueueId", pendSize.toString());
            pendingManagerResponse.put(pendSize.toString(), httpExchange);
            String reqType = requestParamValue.get("type").toString();
//            setChanged();
//            notifyObservers(requestParamValue);
            switch (reqType) {
                case "login":
                case "set":
                case "change":
                    usersManager.update(requestParamValue);
                    break;
                case "execute":
                    paymentManager.update(requestParamValue);
                    break;
            }
            //sendDefaultResponse(httpExchange,"{\"asd\":\"asd\"}");
        }


//        private String handlePostRequest(HttpExchange httpExchange) throws IOException {
//            InputStream bod = httpExchange.getRequestBody();
//            BufferedReader br = new BufferedReader(new InputStreamReader(bod));
//            StringBuilder reqBody = new StringBuilder();
//            String line = br.readLine();
//            while (line != null) {
//                reqBody.append(line);
//                line = br.readLine();
//            }
//            logger.info(reqBody.toString());
//            return reqBody.toString();
//        }
    }
}
