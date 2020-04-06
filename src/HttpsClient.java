import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.io.*;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class HttpsClient {

    public static void main(String[] args) {
        //testIt();
        try {
            //PaymentServer server = new PaymentServer();
//            WebConnector wc = new WebConnector();
//            wc.printAccessToken();
//            String ord = wc.setOrder();
//            System.out.println(ord);
//            wc.getExecuteLink(ord);
            //System.out.println(wc.getOrderApprove(ord));
            PaymentManager paymentManager = new PaymentManager();
            paymentManager.processPayRequest("test", 0.02);
            //paymentManager.processPayRequest("test");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void testIt() {

        String https_url = "https://www.google.com/";
        URL url;
        try {

            url = new URL(https_url);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            //dump all the content
            System.out.println(getContent(con));

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getContent(HttpsURLConnection con){
        try {
            Scanner scanner = new Scanner(con.getInputStream());
            StringBuilder dataString = new StringBuilder();

            while (scanner.hasNextLine()) {
                dataString.append(scanner.nextLine());
            }

            return dataString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
