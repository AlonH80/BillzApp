import com.google.gson.internal.LinkedTreeMap;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server extends Observable {
    private HttpServer server;
    private Logger logger;
    private static final String resourcesPath = "src/test/resources/"; //"/Users/alonhartanu/Desktop/Java/PaymentComponent/WebResources";
    private static final String UiPath = "UI/";
    private PaymentManager paymentManager;
    private UsersManager usersManager;
    private MessageManager messageManager;
    private ApartsManager apartsManager;
    private LinkedTreeMap<String, HttpExchange> pendingManagerResponse;

    public Server() throws Exception {
        logger = Utils.getLogger();
        usersManager = new UsersManager(this);
        messageManager = new MessageManager(this);
        paymentManager = new PaymentManager(this);
        apartsManager = new ApartsManager();
        pendingManagerResponse = new LinkedTreeMap<>();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 8001), 0);
        //server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
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
        byte[] bs = response.getBytes("UTF-8");
        httpExchange.sendResponseHeaders(200, bs.length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(bs);
        os.flush();
        os.close();
    }


    protected class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try {
                if ("GET".equals(httpExchange.getRequestMethod())) {
                    String requestParamValue = handleGetRequest(httpExchange);
                    handleGetResponse(httpExchange, requestParamValue);

                } else if ("POST".equals(httpExchange.getRequestMethod())) {
                    HashMap<String, Object> requestParamValue = handlePostRequest(httpExchange);
                    handlePostResponse(httpExchange, requestParamValue);
                }
            } catch (Exception e) {
                logger.warning(e.getMessage());
                Arrays.stream(e.getStackTrace()).forEach(st -> logger.warning(st.toString()));
            }
        }

        private void handleGetResponse(HttpExchange httpExchange, String htmlContent) throws IOException {
            if (htmlContent.isEmpty()) {
                htmlContent = fileToString(String.format("%s/index.html", UiPath));
            }
            sendDefaultResponse(httpExchange, htmlContent);
        }

        private String handleGetRequest(HttpExchange httpExchange) throws IOException {
            String requestURI = httpExchange.getRequestURI().toString();
            if (requestURI.contains("favicon")||requestURI.contains("compass")) {
                return "";
            }
            if (requestURI.matches("/")) {
                return fileToString(String.format("%s/%s", "UI", "register.html"));
            }
            else if (requestURI.endsWith(".png") || requestURI.endsWith(".ico") || requestURI.toLowerCase().contains("fontawesome")) {
                returnImage(httpExchange, UiPath + requestURI);
                return "";
            }
            return fileToString(String.format("%s%s", "UI", requestURI).split("\\?")[0]);
        }




        public void returnImage(HttpExchange httpExchange, String imgPath) throws IOException {
            File imgFile = new File(imgPath);
            if(imgFile.exists()) {
                httpExchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                OutputStream out = httpExchange.getResponseBody();
                FileInputStream in = new FileInputStream(imgFile);
                byte[] bs = in.readAllBytes();
                httpExchange.sendResponseHeaders(200, bs.length);
                // copy from in to out
                out.write(bs);
                out.flush();
                out.close();
                in.close();
            }else {
                throw new FileNotFoundException();
            }
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

        synchronized private void handlePostResponse(HttpExchange httpExchange, HashMap<String, Object> requestParamValue) throws Exception {
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            Integer pendSize = pendingManagerResponse.size();
            requestParamValue.put("pendQueueId", pendSize.toString());
            pendingManagerResponse.put(pendSize.toString(), httpExchange);
            String reqType = requestParamValue.get("type").toString();
            switch (reqType) {
                case "login":
                case "set":
                case "change":
                    usersManager.update(requestParamValue);
                    break;
                case "execute":
                    paymentManager.update(requestParamValue);
                    break;
                case "messages":
                    List res = messageManager.getMessages(requestParamValue.get("userId").toString());
                    sendDefaultResponse(httpExchange,Utils.listToJson(res));
                    break;
                case "addSupplier":
                    apartsManager.addSupplierToApartment(requestParamValue.get("apartmentId").toString(),requestParamValue.get("billOwner").toString(),Enum.valueOf(Supplier.TYPE.class,requestParamValue.get("supplier").toString().toUpperCase()));
                    break;
            }
        }


        public String fileToString(String filePath) throws IOException {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            int nextCh = br.read();
            while (nextCh != -1) {
                sb.append((char) nextCh);
                nextCh = br.read();
            }
            return sb.toString();
        }
    }
}
