import java.util.Map;

public class HttpsClient {

    public static void main(String[] args) {
        try {
            new Server();
            System.out.println(Utils.generateDateTimeStamp());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void printMap(Map<?, ?> map) {
        map.forEach((k, v)-> System.out.println(String.format("%s: %s", k.toString(), v.toString())));
    }
}
