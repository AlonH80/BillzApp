import java.io.File;
import java.nio.file.Files;
import java.util.Map;

public class HttpsClient {

    public static void main(String[] args) {
        try {
            Files.list(new File(System.getProperty("user.dir") + "/resources").toPath()).forEach(System.out::println);
            Files.list(new File(System.getProperty("user.dir") + "/target").toPath()).forEach(System.out::println);
            Files.list(new File(System.getProperty("user.dir") + "/target/classes").toPath()).forEach(System.out::println);
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
