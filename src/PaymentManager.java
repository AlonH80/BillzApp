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
    private HashMap<String, String> componentConfig;
    private HashMap<String, HashMap<String, String>> infoCache;
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
        componentConfig = new HashMap<>();
        waitingApproved = new HashMap<>();
        HashMap<String, Object> tmpComponentConfig = (new Gson()).fromJson(new JsonReader(new FileReader(confPath)), HashMap.class);
        tmpComponentConfig.forEach((k,v)->componentConfig.put(k, v.toString()));
        logger.info(String.format("ComponentConfig: %s", componentConfig.toString()));
        webConnector.setLogger(logger);
        checkFileExist(componentConfig.get("orderTemplatePath"));
        verifyAccessToken();
    }

    private void initLogger() throws Exception {
        String logFullPath = String.format("%s/pm_%s", logsPath, (new SimpleDateFormat("yy_MM_dd___HH_mm")).format(Calendar.getInstance().getTime()));
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] {%2$s} - %4$s -  %5$s%6$s%n");
        logger = Logger.getLogger(PaymentManager.class.getName());
        FileHandler fh = new FileHandler(logFullPath);
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }

    public void processPayRequest(String userId, Double amount) throws Exception{
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
        waitingApproved.put(respMap.get("id").toString(), relevantLinks);

    }

    private String generateInvoice() throws Exception {
        String newInvoice = String.format("%04d", Integer.parseInt(componentConfig.get("lastInvoice")) + 1);
        componentConfig.replace("lastInvoice", newInvoice);
        saveConfig();
        return newInvoice;
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
            webConnector.executeOrder(waitingApproved.get(args.get("paymentId")).get("execute"), args.get("PayerID"));
        }
        catch (Exception e){
            Arrays.stream(e.getStackTrace()).forEach(st->logger.warning(st.toString()));
        }
    }
}
