public class HttpsMain {
    public static void main(String[] args) {
        try {
            PaymentServer server = new PaymentServer();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
