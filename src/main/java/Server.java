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
    private final static String serverAddress = "https://billz-app.herokuapp.com";
    private static String resourcesPath;
    private static String UiPath;
    private PaymentManager paymentManager;
    private UsersManager usersManager;
    private MessageManager messageManager;
    private ApartsManager apartsManager;
    private LinkedTreeMap<String, HttpExchange> pendingManagerResponse;

    public Server() throws Exception {
        logger = Utils.getLogger();
        Map configs = Utils.loadConfigs();
        resourcesPath = configs.get("resourcesPath").toString();
        UiPath= configs.get("UiPath").toString();
        usersManager = new UsersManager(this);
        messageManager = new MessageManager(this);
        paymentManager = new PaymentManager(this);
        apartsManager = new ApartsManager();
        pendingManagerResponse = new LinkedTreeMap<>();
        int port;
        if (configs.get("port").toString().equals("PORT")) {
            port = Integer.parseInt(System.getenv("PORT"));
        }
        else {
            port = Integer.parseInt(configs.get("port").toString());
        }
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port), 0);
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
        if (response.length() == 0){
            HashMap<String, String> defaultMap = new HashMap<>(1);
            defaultMap.put("status", "success");
            response = Utils.mapToJson(defaultMap);
        }
        byte[] bs = response.getBytes("UTF-8");
        httpExchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        httpExchange.sendResponseHeaders(200, bs.length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(bs);
        os.flush();
        os.close();
    }

    private void sendRedirect(HttpExchange httpExchange, String redirectUrl) throws Exception {
        httpExchange.getResponseHeaders().set("Location", String.format("%s/%s", serverAddress, redirectUrl));
        httpExchange.sendResponseHeaders(302, 0);
    }


    protected class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try {
                if ("GET".equals(httpExchange.getRequestMethod())) {
                    String requestParamValue = handleGetRequest(httpExchange);
                    if (!requestParamValue.isEmpty())
                        handleGetResponse(httpExchange, requestParamValue);

                } else if ("POST".equals(httpExchange.getRequestMethod())) {
                    HashMap<String, Object> requestParamValue = handlePostRequest(httpExchange);
//                    if(!requestParamValue.isEmpty())
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

        private String handleGetRequest(HttpExchange httpExchange) throws Exception {
            String requestURI = httpExchange.getRequestURI().toString();
            if(requestURI.contains("joinApart")){
                String apartmentId = requestURI.split("\\?")[2];
                String userId = requestURI.split("\\?")[3];
                apartsManager.addUserToApartment(apartmentId,userId);
                //sendDefaultResponse(httpExchange,"");
                messageManager.addMessage(userId, apartmentId, "user_joined", String.format("%s has joined the apartment!", userId));
                return fileToString(String.format("%s/%s", UiPath, "login.html"));
            }
            else if(requestURI.contains("paymentApproved")){
                String quer = requestURI.split("\\?")[1];
                String[] querParams = quer.split("&");
                HashMap<String, String> custMap = new HashMap<>();
                Arrays.stream(querParams).forEach(par -> custMap.put(par.split("=")[0], par.split("=")[1]));
                logger.info(Utils.mapToJson(custMap));
                Map<String, String> transac = paymentManager.logTransaction(custMap.get("paymentId"));
                logger.info(Utils.mapToJson(transac));
                apartsManager.updateBillAfterTransaction(transac.get("userIdFrom"), transac.get("userIdTo"), Double.parseDouble(transac.get("amount")), transac.get("supplier"), transac.get("dDay"));
                return fileToString(String.format("%s/%s", UiPath, "index.html"));
            }
            else if(requestURI.contains("paymentId")) {
                String quer = requestURI.split("\\?")[1];
                String[] querParams = quer.split("&");
                HashMap<String, String> custMap = new HashMap<>();
                Arrays.stream(querParams).forEach(par -> custMap.put(par.split("=")[0], par.split("=")[1]));
                logger.info(Utils.mapToJson(custMap));
                paymentManager.executeOrder(custMap.get("paymentId"), custMap.get("PayerID"));
                return fileToString(String.format("%s/%s", UiPath, "index.html"));
            }
            else if (requestURI.contains("favicon") || requestURI.contains("compass")) {
                sendDefaultResponse(httpExchange, "");
                return "";
            }
            else if (requestURI.matches("/")) {
                return fileToString(String.format("%s/%s", UiPath, "login.html"));
            } else if (requestURI.endsWith(".png") || requestURI.endsWith(".ico") || requestURI.toLowerCase().contains("fontawesome")) {
                returnImage(httpExchange, UiPath + requestURI);
                return "";
            }
            return fileToString(String.format("%s%s", UiPath, requestURI).split("\\?")[0]);
        }


        public void returnImage(HttpExchange httpExchange, String imgPath) throws IOException {
            File imgFile = new File(imgPath);
            if (imgFile.exists()) {
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
            } else {
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
            List resLst;
            Map resMap;
            String resStr;
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            Integer pendSize = pendingManagerResponse.size();
            requestParamValue.put("pendQueueId", pendSize.toString());
            pendingManagerResponse.put(pendSize.toString(), httpExchange);
            String reqType = requestParamValue.get("type").toString();
            String userId = requestParamValue.get("userId").toString();
            String apartmentId = requestParamValue.get("apartmentId").toString();
            switch (reqType) {
                case "login":
                case "set":
                case "change":
                    usersManager.update(requestParamValue);
                    break;
                case "leaveApartment":
                    apartsManager.leaveApartment(userId);
                    sendDefaultResponse(httpExchange, "");
                    break;
//                case "execute":
//                    paymentManager.executeOrder(requestParamValue.get("").toString(), requestParamValue.get("").toString());
//                    break;
                case "messages":
                    resLst = messageManager.getMessages(apartmentId);
                    sendDefaultResponse(httpExchange, Utils.listToJson(resLst));
                    break;
                case "addBill":
                    apartsManager.addBill(apartmentId, requestParamValue.get("dDay").toString(), requestParamValue.get("amount").toString(), requestParamValue.get("billType").toString(), userId);
                    messageManager.addMessage(userId, apartmentId, "bill", String.format("New bill added for supplier %s, amount: %s", requestParamValue.get("billType").toString(), requestParamValue.get("amount").toString()));
                    sendDefaultResponse(httpExchange, "");
                    break;
                case "getBills":
                    resLst = apartsManager.getBills(userId, apartmentId);
                    sendDefaultResponse(httpExchange, Utils.listToJson(resLst));
                    break;
                case "addSupplier":
                    apartsManager.addSupplierToApartment(apartmentId, requestParamValue.get("billOwner").toString(), Enum.valueOf(Supplier.TYPE.class, requestParamValue.get("supplier").toString().toUpperCase()), (Map<String, Object>) requestParamValue.get(("partsMap")));
                    messageManager.addMessage(userId,apartmentId, "new_supplier",String.format("%s has added new supplier: %s", userId, requestParamValue.get("supplier").toString()));
                    sendDefaultResponse(httpExchange, "");
                    break;
                case "editSupplier":
                    apartsManager.editSupplier(apartmentId, requestParamValue.get("billOwner").toString(), Enum.valueOf(Supplier.TYPE.class, requestParamValue.get("supplier").toString().toUpperCase()), (Map<String, Object>) requestParamValue.get(("partsMap")));
                    //messageManager.addMessage(userId,apartmentId, "edit_supplier",String.format("%s has added new supplier: %s", userId, requestParamValue.get("supplier").toString()));
                    sendDefaultResponse(httpExchange, "");
                    break;
                case "getSuppliers":
                    resLst = apartsManager.getSuppliers(apartmentId);
                    sendDefaultResponse(httpExchange, Utils.listToJson(resLst));
                    break;
                case "changeSetting":
                    usersManager.updateSetting(userId, requestParamValue.get("setting").toString(), requestParamValue.get("value").toString());
                    sendDefaultResponse(httpExchange, "");
                    break;
                case "createApartment":
                    resStr = apartsManager.createApartment(userId);
                    sendDefaultResponse(httpExchange, resStr);
                    break;
                case "roommates":
                    resLst = apartsManager.getRoommates(apartmentId, userId);
                    sendDefaultResponse(httpExchange, Utils.listToJson(resLst));
                    break;
                case "addRoommate":
                    //apartsManager.addRoommate(apartmentId,userId);
                    sendDefaultResponse(httpExchange, "");
                    break;
                case "balance":
                    resMap = apartsManager.getApartmentBalances(apartmentId);
                    sendDefaultResponse(httpExchange, Utils.mapToJson(resMap));
                    break;
                case "payment":
                    String payTo =  requestParamValue.get("payTo").toString();
                    Double amount = Double.parseDouble(requestParamValue.get("amount").toString());
                    String supplierType = requestParamValue.get("supplier").toString();
                    String method = requestParamValue.get("payMethod").toString();
                    String dDay = requestParamValue.get("dDay").toString();
                    Map<String, String> transferTmpResponse = paymentManager.transferMoney(userId, payTo, amount, supplierType, method, dDay);
                    if (!method.toLowerCase().equals("paypal")) {
                        apartsManager.updateBillAfterTransaction(transferTmpResponse.get("userIdFrom"), transferTmpResponse.get("userIdTo"), Double.parseDouble(transferTmpResponse.get("amount")), transferTmpResponse.get("supplier"), dDay);
                    }
                    sendDefaultResponse(httpExchange, Utils.mapToJson(transferTmpResponse));
                    break;
                case "userDetails":
                    resMap = usersManager.getInfoOnUser(userId);
                    sendDefaultResponse(httpExchange, Utils.mapToJson(resMap));
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
