import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.io.*;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class HttpsClient {

    public static void main(String[] args) {
        try {
            PaymentManager paymentManager = new PaymentManager();
            //paymentManager.transferMoney("testFrom", "testTo", 1.0);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
