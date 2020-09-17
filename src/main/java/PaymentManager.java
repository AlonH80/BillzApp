import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PaymentManager {
    private Server server;
    private Logger logger;
    private WebConnector webConnector;
    private MongoConnector mongoConnector;
    private HashMap<String, String> componentConfig;
    private HashMap<String, HashMap<String, Object>> recordPending;
    private HashMap<String, HashMap<String, String>> waitingApproved;
    private final static String confPath = System.getProperty("user.dir") +"/target/classes/pp_config.json";
    private final static String logsPath = "logs/";

    public PaymentManager(Server server) throws Exception {
        this.server = server;
        componentConfig = new HashMap<>();
        waitingApproved = new HashMap<>();
        recordPending = new HashMap<>();
        initLogger();
        initConfigFile();
        initConnectors();
    }

    private void initLogger() throws Exception {
        logger = Utils.getLogger();
    }

    private void initConnectors() throws Exception {
//        paymentServer = new PaymentServer();
//        logger.info("Server started on port 8001");
//        paymentServer.addObserver(this);
//        paymentServer.setLogger(logger);
        webConnector = new WebConnector();
        webConnector.setLogger(logger);
        verifyAccessToken();
        mongoConnector = MongoConnector.getInstance();
        mongoConnector.setLogger(logger);
//        // Initialise the keystore
//        char[] password = "PaymentServer".toCharArray();
//        KeyStore ks = KeyStore.getInstance("JKS");
//        FileInputStream fis = new FileInputStream(Utils.loadConfigs().get("resourcesPath") + "/ps.keystore"); //TODO: add to config file
//        ks.load(fis, password);
//
//        // Set up the key manager factory
//        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//        kmf.init(ks, password);
//
//        // Set up the trust manager factory
//        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
//        tmf.init(ks);

    }

    private void initConfigFile() throws Exception {
        componentConfig = Utils.loadConfigs();
        initPayPalConfig();

    }

    private void initPayPalConfig() throws Exception {
        try {
            String user_dir = System.getProperty("user.dir");
            HashMap<String, Object> tmpConfig = Utils.jsonFileToMap(confPath);
            tmpConfig.forEach((k, v) -> componentConfig.put(k, v.toString()));
            componentConfig.put("orderTemplatePath", user_dir + componentConfig.get("orderTemplatePath"));
            componentConfig.put("sendTemplatePath", user_dir + componentConfig.get("sendTemplatePath"));
        }
        catch (Exception e) {
            logger.warning("Unable to load conf file");
        }
        logger.info(String.format("ComponentConfig: %s", componentConfig.toString()));
    }

    public Map<String, String> transferMoney(String userIdFrom, String userIdTo, Double amount, String supplierType, String method, String dDay) throws Exception {
        verifyAccessToken();
        logger.info(String.format("userIdFrom: %s", userIdFrom));
        logger.info(String.format("userIdTo: %s", userIdTo));
        logger.info(String.format("amount: %.2f", amount));
        if (method.toLowerCase().equals("paypal")) {
            HashMap<String, String> waitMap = new HashMap<>();
            Object queryUserMailTo = mongoConnector.getUser(userIdTo).get("paypal");
            Object queryUserMailFrom = mongoConnector.getUser(userIdFrom).get("paypal");
            if (queryUserMailTo != null && queryUserMailFrom != null) {
                String userToMail = queryUserMailTo.toString();
                String userFromMail = queryUserMailFrom.toString();
                String waitApprovedKey = processPayRequest(userIdTo, amount);
                waitingApproved.get(waitApprovedKey).put("userToMail", userToMail);
                waitingApproved.get(waitApprovedKey).put("userFromMail", userFromMail);
                waitingApproved.get(waitApprovedKey).put("amount", amount.toString());
                waitingApproved.get(waitApprovedKey).put("userIdFrom", userIdFrom);
                waitingApproved.get(waitApprovedKey).put("userIdTo", userIdTo);
                waitingApproved.get(waitApprovedKey).put("supplier", supplierType);
                waitingApproved.get(waitApprovedKey).put("payMethod", method);
                return waitingApproved.get(waitApprovedKey);
            } else {
                waitMap.put("status", "fail");
                waitMap.put("reason", "");
                if (queryUserMailFrom == null) {
                    waitMap.put("reason", String.format("%s hasn't update his PayPal account", userIdFrom));
                }
                if (queryUserMailTo == null) {
                    waitMap.put("reason", waitMap.get("reason") + ", " + String.format("%s hasn't update his PayPal account", userIdTo));
                }
                return waitMap;
            }
        }
        else {
            HashMap<String, String> payMap = new HashMap<>();
            payMap.put("amount", amount.toString());
            payMap.put("userIdFrom", userIdFrom);
            payMap.put("userIdTo", userIdTo);
            payMap.put("supplier", supplierType);
            payMap.put("payMethod", method);
            recordTransaction(userIdFrom, userIdTo, amount, supplierType, method);
            return payMap;
        }
    }

    private String processPayRequest(String userIdTo, Double amount) throws Exception{
        String order = createOrder(userIdTo, amount);
        logger.info(order);
        String webResp = webConnector.setOrder(order);
        HashMap<String, Object> respMap = (new Gson()).fromJson(webResp, HashMap.class);
        HashMap<String, String> relevantLinks = new HashMap<>(2);
        ArrayList<Object> linksAr = (ArrayList<Object>)respMap.get("links");
        linksAr.stream().forEach(link-> {
            String currRel = ((LinkedTreeMap<String, Object>)link).get("rel").toString();
            String currLink = ((LinkedTreeMap<String, Object>)link).get("href").toString();
            if (currRel.equals("approval_url")){
                relevantLinks.put("approval_url", currLink);
            }
            else if (currRel.equals("execute")){
                relevantLinks.put("execute", currLink);
            }
        });
        String waitApprovedKey = respMap.get("id").toString();
        waitingApproved.put(waitApprovedKey, relevantLinks);
        return waitApprovedKey;
    }

    private void processGetPayRequest(String userMail, Double amount) throws Exception {
        HashMap<String, Object> getPayMap = getMapFromJsonFile(componentConfig.get("sendTemplatePath").toString());
        LinkedTreeMap<String, Object> senderBatch = (LinkedTreeMap<String, Object>)(getPayMap.get("sender_batch_header"));
        ArrayList<LinkedTreeMap<String, Object>> items = (ArrayList<LinkedTreeMap<String, Object>>)getPayMap.get("items");
        LinkedTreeMap<String, Object> item = items.get(0);
        ((LinkedTreeMap<String, Object>)item.get("amount")).put("value", amount);
        item.put("receiver", userMail);
        senderBatch.put("sender_batch_id", generateSenderBatchId());
        getPayMap.put("sender_batch_header", senderBatch);
        getPayMap.put("items", items);
        String payMapAfterUpdate = (new Gson()).toJson(getPayMap);
        String resp = webConnector.sendPayment(payMapAfterUpdate);
    }

    private String generateInvoice() throws Exception {
        Integer invoiceInt = Integer.parseInt(componentConfig.get("lastInvoice")) + 1;
        String newInvoice = String.format("get_%04d", invoiceInt);
        componentConfig.replace("lastInvoice", invoiceInt.toString());
        saveConfig();
        return newInvoice;
    }

    private String generateSenderBatchId() throws Exception {
        Integer sendBatchInt = Integer.parseInt(componentConfig.get("lastSendBatch")) + 1;
        String newSendBatch = String.format("send_%04d", sendBatchInt);
        componentConfig.replace("lastSendBatch", sendBatchInt.toString());
        saveConfig();
        return newSendBatch;
    }

    private String createOrder(String userIdTo, Double amount) throws Exception {
        HashMap<String, Object> orderMap = (Utils.jsonFileToMap(componentConfig.get("orderTemplatePath")));
        logger.info(orderMap.toString());
        LinkedTreeMap<String, Object> transactions = (LinkedTreeMap<String, Object>)((ArrayList<Object>)orderMap.get("transactions")).get(0);
        ArrayList<LinkedTreeMap<String, Object>> newTrans = new ArrayList<>(1);
        newTrans.add(updateTransactions(transactions, userIdTo, amount));
        orderMap.put("transactions", newTrans);
        logger.info(orderMap.toString());
        return Utils.mapToJson(orderMap);
    }

    private LinkedTreeMap<String, Object> updateTransactions(LinkedTreeMap<String, Object> transactions, String userIdTo, Double amount) throws Exception {
        String amountStr = String.format("%.2f", amount);
        transactions.put("invoice_number", generateInvoice());
        LinkedTreeMap<String, Object> amountMap = (LinkedTreeMap<String, Object>)transactions.get("amount");
        LinkedTreeMap<String, Object> itemsList = (LinkedTreeMap<String, Object>)transactions.get("item_list");
        LinkedTreeMap<String, Object> shippingAddress = (LinkedTreeMap<String, Object>)itemsList.get("shipping_address");
        amountMap.put("total", amount);
        ((LinkedTreeMap<String, Object>)amountMap.get("details")).put("subtotal", amount);
        ((LinkedTreeMap<String, Object>)((ArrayList<Object>)itemsList.get("items")).get(0)).put("price", amount);
        shippingAddress.put("recipient_name", userIdTo);
        return transactions;
    }

    private String getJsonTemplate(){
        return "";
    }

    private void checkFileExist(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException(filePath);
        }
    }

    private void verifyAccessToken() throws Exception {
        String nonce = componentConfig.get("nonce");
        Integer expires = (int)Float.parseFloat(componentConfig.get("expires_in"));

        logger.info(String.format("Expires seconds %d", expires));

        String dtFormat = "yyyy-MM-dd HH:mm:ss";
        String realDT = nonce.substring(0, dtFormat.length()).replace('T', ' ');
        SimpleDateFormat formatter = new SimpleDateFormat(dtFormat);
        Calendar currTime = Calendar.getInstance();
        Calendar tokenTime = Calendar.getInstance();
        logger.info(String.format("currTime (Calender): %s", currTime.getTime()));
        tokenTime.setTime(formatter.parse(realDT));
        logger.info(String.format("tokenTime (Calender): %s", tokenTime.getTime()));
        tokenTime.add(Calendar.SECOND, expires);
        logger.info(String.format("tokenTime (Calender): %s", tokenTime.getTime()));
        logger.info(String.format("currTime.after(tokenTime) %s", currTime.after(tokenTime)));
        if (currTime.after(tokenTime)) {
            HashMap<String, Object> newTokenMap = webConnector.getNewAccessToken();
            componentConfig.put("access_token", newTokenMap.get("access_token").toString());
            componentConfig.put("nonce", newTokenMap.get("nonce").toString());
            componentConfig.put("expires_in", newTokenMap.get("expires_in").toString());
            saveConfig();
        }
        else{
            webConnector.setAccessToken(componentConfig.get("access_token"));
        }
    }

    private void saveConfig() throws Exception {
        FileWriter fileWriter = new FileWriter(confPath);
        String jsonString = Utils.mapToJson(componentConfig);
        fileWriter.write(jsonString);
        fileWriter.close();
    }

    public String executeOrder(String waitId, String payerId) throws Exception {
        HashMap<String, String> paymentMap = waitingApproved.get(waitId);
        webConnector.executeOrder(paymentMap.get("execute"), payerId);
        String userMailTo = paymentMap.get("userToMail");
        Double amount = Double.parseDouble(paymentMap.get("amount"));
        processGetPayRequest(userMailTo, amount);
        return "";
    }

//    public void update(Object arg) {
//        HashMap<String, String> args = (HashMap<String, String>)arg;
//        logger.info(args.toString());
//        try {
//            if (args.get("type").equals("execute")) {
//                if (waitingApproved.keySet().contains(args.get("paymentId"))) {
//                    HashMap<String, String> paymentMap = waitingApproved.get(args.get("paymentId"));
//                    webConnector.executeOrder(paymentMap.get("execute"), args.get("PayerID"));
//                    String userMailTo = paymentMap.get("userToMail");
//                    Double amount = Double.parseDouble(paymentMap.get("amount"));
//                    processGetPayRequest(userMailTo, amount);
//                    mongoConnector.recordTransaction(paymentMap.get("userIdFrom"),
//                            paymentMap.get("userIdTo"),
//                            Double.parseDouble(paymentMap.get("amount")));
//                    waitingApproved.remove(args.get("paymentId"));
//                }
//                else{
//                    logger.warning(String.format("%s is not waiting approved", args.get("paymentId")));
//                }
//            }
//            else if (args.get("type").equals("transferMoney")) {
//                String waitApprovedKey = transferMoney(args.get("userIdFrom"),
//                              args.get("userIdTo"),
//                              Double.parseDouble(args.get("amount"))
//                );
//                if (waitApprovedKey!=null) {
//                    String responeJson = (new Gson()).toJson(waitingApproved.get(waitApprovedKey));
//                    server.sendManagerResponse(args.get("pendQueueId"), responeJson);
//                }
//                else{
//                    Map<String, String> errorMap = new HashMap<>(1);
//                    errorMap.put("error", "payPalMail not specified");
//                    server.sendManagerResponse(args.get("pendQueueId"), (new Gson()).toJson(errorMap));
//                }
//            }
//        }
//        catch (Exception e){
//            logger.warning(e.getMessage());
//            Arrays.stream(e.getStackTrace()).forEach(st->logger.warning(st.toString()));
//        }
//    }

    private HashMap<String, Object> getMapFromJsonFile(String jsonPath) throws Exception {
        JsonReader jsonReader = new JsonReader(new FileReader(jsonPath));
        HashMap<String, Object> jsonMap = (new Gson()).fromJson(jsonReader, HashMap.class);
        return jsonMap;
    }

    public Map<String, String> logTransaction(String paymentId) {
        HashMap<String, String> paymentMap = waitingApproved.get(paymentId);
        recordTransaction(paymentMap.get("userIdFrom"),
                paymentMap.get("userIdTo"),
                Double.parseDouble(paymentMap.get("amount")),
                paymentMap.get("supplier"), "Paypal");
        //waitingApproved.remove(paymentId);
        return paymentMap;
    }

    private void recordTransaction(String userIdFrom, String userIdTo, double amount, String supplier, String method) {
        mongoConnector.recordTransaction(userIdFrom, userIdTo, amount, supplier, method);

    }
}
