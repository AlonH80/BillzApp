import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import java.util.logging.Logger;


public class WebConnector {

    private static final String httpsOrderURL= "https://api.sandbox.paypal.com/v1/payments/payment";    // TODO: create config file
    private static final String httpsSendPayURL = "https://api.sandbox.paypal.com/v1/payments/payouts";
    private static final String httpsRequestTokenURL = "https://api.sandbox.paypal.com/v1/oauth2/token";    // TODO: create config file
    private static String clientID; // TODO: create SECURED config file
    private static String secret; // TODO: create SECURED config file
    private String accessToken;
    private Logger logger;

    static {
        try {
            //JsonReader jsonReader = new JsonReader(new FileReader("/Users/alonhartanu/Desktop/Java/PaymentComponent/src/keys.encrypt"));
            JsonReader jsonReader = new JsonReader(new FileReader("resources/keys.encrypt"));
            HashMap<String, Object> jsonMap = (new Gson()).fromJson(jsonReader, HashMap.class);
            clientID = jsonMap.get("clientID").toString();
            secret = jsonMap.get("secret").toString();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

//    public WebConnector() throws Exception {
//
//    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setAccessToken(String accessToken){
        this.accessToken = accessToken;
    }

    public HashMap<String, Object> getNewAccessToken() throws Exception {
        URL url = new URL(httpsRequestTokenURL);
        HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Accept-Language", "en_US");
        String auth = clientID + ":" + secret;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + new String(encodedAuth);
        con.setRequestProperty("Authorization", authHeaderValue);
        HashMap<String,Object> respMap = (new Gson()).fromJson(sendPostRequest(con, "grant_type=client_credentials"), HashMap.class);
        this.accessToken = respMap.get("access_token").toString();
        return respMap;
    }



    public String setOrder(String order) throws Exception{
        HttpsURLConnection con = getDefaultConnection(httpsOrderURL);
        con.setRequestProperty("Content-length", String.valueOf(order.length()));
        return sendPostRequest(con, order);
    }

    public String getPayId(String payResponeJson){
        Gson gson = new Gson();
        HashMap<String, Object> jsonMap = gson.fromJson(payResponeJson, HashMap.class);
        return jsonMap.get("id").toString();
    }

    public String getExecuteLink(String payResponeJson) throws Exception {
        Gson gson = new Gson();
        HashMap<String, Object> jsonMap = gson.fromJson(payResponeJson, HashMap.class);
        ArrayList<LinkedTreeMap<String, Object>> linksList = (ArrayList<LinkedTreeMap<String, Object>>) jsonMap.get("links");
        LinkedTreeMap<String, Object> relevantLinks = linksList.get(2);
        String execLink = relevantLinks.get("href").toString();
        LinkedTreeMap<String, Object> approvalLinks = linksList.get(1);
        String approvalLink = approvalLinks.get("href").toString();
        logger.info("Approval link: " + approvalLink);
        //logger.info("Exec link: " + execLink);
        return execLink;
    }

    public String executeOrder(String executeLink, String payId) throws Exception {
        HttpsURLConnection con = getDefaultConnection(executeLink);
        HashMap<String, String> reqJson = new HashMap<>(1);
        reqJson.put("payer_id", payId);
        logger.info(String.format("Executing link: %s", executeLink));
        return sendPostRequest(con, (new Gson()).toJson(reqJson));
    }

    public String sendPayment(String sendPayDetails) throws Exception {
        HttpsURLConnection con = getDefaultConnection(httpsSendPayURL);
        logger.info("Send pay info");
        logger.info(sendPayDetails);
        String resp = sendPostRequest(con, sendPayDetails);
        return resp;
    }

    private String sendPostRequest(HttpsURLConnection con, String content) throws Exception {
        con.setDoOutput(true);
        con.setDoInput(true);

        DataOutputStream output = new DataOutputStream(con.getOutputStream());
        output.writeBytes(content);
        output.close();
        DataInputStream input;
        if (con.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST)
            input = new DataInputStream(con.getInputStream());
        else
            input = new DataInputStream(con.getErrorStream());
        StringBuilder resp = new StringBuilder();

        for( int c = input.read(); c != -1; c = input.read())
            resp.append((char)c);
        input.close();
        logger.info(String.format("Https POST response: %s", resp.toString()));
        return resp.toString();
    }

    private HttpsURLConnection getDefaultConnection(String url) throws Exception {
        URL conUrl = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection)conUrl.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type","application/json");
        con.setRequestProperty("Authorization", "Bearer " + accessToken);

        return con;
    }

    private void diagnoseResponse(String resp){
        return;
    }
}
