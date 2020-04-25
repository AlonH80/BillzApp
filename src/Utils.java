import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {

    public static String generateDateTimeStamp() {
        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        df.setTimeZone(TimeZone.getTimeZone("Asia/Jerusalem"));
        return df.format(new Date());
    }

    public static String mapToJson(Map<?, ?> map) {
        return (new Gson()).toJson(map);
    }

    public static Map<String, Object> jsonToMap(String jsonStr) {
        return (new Gson()).fromJson(jsonStr, HashMap.class);
    }

    public static ArrayList<Object> jsonToList(String jsonStr) {
        return (new Gson()).fromJson(jsonStr, ArrayList.class);
    }

    public static String listToJson(List<?> list) {
        return (new Gson()).toJson(list);
    }

    public static HashMap<String,Object> jsonFileToMap(String filePath) throws Exception {
        return (new Gson()).fromJson(new JsonReader(new FileReader(filePath)), HashMap.class);
    }

    public static boolean containsLowerCase(String str) {
        boolean res = false;

        for (char ch : str.toCharArray()) {
            if (Character.isLowerCase(ch)) {
                res = true;
                break;
            }
        }

        return res;
    }

    public static boolean containsUpperCase(String str) {
        boolean res = false;

        for (char ch : str.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                res = true;
                break;
            }
        }

        return res;
    }
}
