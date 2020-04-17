import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PaymentManager implements Observer {
    private PaymentServer paymentServer;
    private Logger logger;
    private WebConnector webConnector;
    private MongoConnector mongoConnector;
    private HashMap<String, String> componentConfig;
    private HashMap<String, HashMap<String, Object>> recordPending;
    private HashMap<String, HashMap<String, String>> waitingApproved;
    private final static String confPath = "/Users/alonhartanu/Desktop/Java/PaymentComponent/src/config.json";
    private final static String logsPath = "/Users/alonhartanu/Desktop/Java/PaymentComponent/logs/";

    public PaymentManager() throws Exception {
        initLogger();
        paymentServer = new PaymentServer();
        logger.info("Server started on port 8001");
        paymentServer.addObserver(this);
        paymentServer.setLogger(logger);
        webConnector = new WebConnector();
        webConnector.setLogger(logger);
        mongoConnector = new MongoConnector();
        mongoConnector.setLogger(logger);
        componentConfig = new HashMap<>();
        waitingApproved = new HashMap<>();
        HashMap<String, Object> tmpComponentConfig = (new Gson()).fromJson(new JsonReader(new FileReader(confPath)), HashMap.class);
        tmpComponentConfig.forEach((k,v)->componentConfig.put(k, v.toString()));
        logger.info(String.format("ComponentConfig: %s", componentConfig.toString()));
        checkFileExist(componentConfig.get("orderTemplatePath"));
        verifyAccessToken();
        recordPending = new HashMap<>();
    }

    private void initLogger() throws Exception {
        File logsDirectory = new File(logsPath);
        if (! logsDirectory.exists()) {
            logsDirectory.mkdir();
        }

        String logFullPath = String.format("%s/pm_%s", logsPath, (new SimpleDateFormat("yy_MM_dd___HH_mm")).format(Calendar.getInstance().getTime()));
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] {%2$s} - %4$s -  %5$s%6$s%n");
        logger = Logger.getLogger(PaymentManager.class.getName());
        FileHandler fh = new FileHandler(logFullPath);
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }

    public String transferMoney(String userIdFrom, String userIdTo, Double amount) throws Exception {
        String waitApprovedKey = null;
        Object queryUserMailTo = mongoConnector.getUser(userIdTo).get("payPalMail");
        Object queryUserMailFrom = mongoConnector.getUser(userIdFrom).get("payPalMail");
        if (queryUserMailTo!=null && queryUserMailFrom!=null){
            String userToMail = queryUserMailTo.toString();
            String userFromMail = queryUserMailFrom.toString();
            waitApprovedKey = processPayRequest(userIdTo, amount);
            waitingApproved.get(waitApprovedKey).put("userToMail", userToMail);
            waitingApproved.get(waitApprovedKey).put("userFromMail", userFromMail);
            waitingApproved.get(waitApprovedKey).put("amount", amount.toString());
            waitingApproved.get(waitApprovedKey).put("userIdFrom", userIdFrom);
            waitingApproved.get(waitApprovedKey).put("userIdTo", userIdTo);
            return waitApprovedKey;
        }

        return waitApprovedKey;
    }

    private String processPayRequest(String userId, Double amount) throws Exception{
        String order = createOrder(amount);
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

    private String createOrder(Double amount) throws Exception {
        HashMap<String, Object> orderMap = (new Gson()).fromJson(new JsonReader(new FileReader(componentConfig.get("orderTemplatePath"))), HashMap.class);
        logger.info(orderMap.toString());
        LinkedTreeMap<String, Object> transactions = (LinkedTreeMap<String, Object>)((ArrayList<Object>)orderMap.get("transactions")).get(0);
        ArrayList<LinkedTreeMap<String, Object>> newTrans = new ArrayList<>(1);
        newTrans.add(updateTransactions(transactions, amount));
        orderMap.put("transactions", newTrans);
        logger.info(orderMap.toString());
        return (new Gson()).toJson(orderMap);
    }

    private LinkedTreeMap<String, Object> updateTransactions(LinkedTreeMap<String, Object> transactions, Double amount) throws Exception {
        String amountStr = String.format("%.2f", amount);
        transactions.put("invoice_number", generateInvoice());
        LinkedTreeMap<String, Object> amountMap = (LinkedTreeMap<String, Object>)transactions.get("amount");
        LinkedTreeMap<String, Object> itemsList = (LinkedTreeMap<String, Object>)transactions.get("item_list");
        amountMap.put("total", amount);
        ((LinkedTreeMap<String, Object>)amountMap.get("details")).put("subtotal", amount);
        ((LinkedTreeMap<String, Object>)((ArrayList<Object>)itemsList.get("items")).get(0)).put("price", amount);
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
        String jsonString = (new Gson()).toJson(componentConfig);
        fileWriter.write(jsonString);
        fileWriter.close();
    }

    @Override
    public void update(Observable o, Object arg) {
        HashMap<String, String> args = (HashMap<String, String>)arg;
        logger.info(args.toString());
        try {
            if (args.get("type").equals("execute")) {
                if (waitingApproved.keySet().contains(args.get("paymentId"))) {
                    HashMap<String, String> paymentMap = waitingApproved.get(args.get("paymentId"));
                    webConnector.executeOrder(paymentMap.get("execute"), args.get("PayerID"));
                    String userMailTo = paymentMap.get("userToMail");
                    Double amount = Double.parseDouble(paymentMap.get("amount"));
                    processGetPayRequest(userMailTo, amount);
                    mongoConnector.recordTransaction(paymentMap.get("userIdFrom"),
                            paymentMap.get("userIdTo"),
                            Double.parseDouble(paymentMap.get("amount")));
                    waitingApproved.remove(args.get("paymentId"));
                }
                else{
                    logger.warning(String.format("%s is not waiting approved", args.get("paymentId")));
                }
            }
            else if (args.get("type").equals("transferMoney")) {
                String waitApprovedKey = transferMoney(args.get("userIdFrom"),
                              args.get("userIdTo"),
                              Double.parseDouble(args.get("amount"))
                );
                if (waitApprovedKey!=null) {
                    String responeJson = (new Gson()).toJson(waitingApproved.get(waitApprovedKey));
                    paymentServer.sendManagerResponse(args.get("pendQueueId"), responeJson);
                }
                else{
                    Map<String, String> errorMap = new HashMap<>(1);
                    errorMap.put("error", "payPalMail not specified");
                    paymentServer.sendManagerResponse(args.get("pendQueueId"), (new Gson()).toJson(errorMap));
                }
            }
        }
        catch (Exception e){
            logger.warning(e.getMessage());
            Arrays.stream(e.getStackTrace()).forEach(st->logger.warning(st.toString()));
        }
    }

    private HashMap<String, Object> getMapFromJsonFile(String jsonPath) throws Exception {
        JsonReader jsonReader = new JsonReader(new FileReader(jsonPath));
        HashMap<String, Object> jsonMap = (new Gson()).fromJson(jsonReader, HashMap.class);
        return jsonMap;
    }
}
