import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

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
}
