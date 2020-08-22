import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Utils {
    private final static String logsPath = "logs/";
    private static Logger logger = null;
    public static String resourcesPath = System.getProperty("user.dir") +"/target/classes";
    public static String confPath = System.getProperty("user.dir") +"/target/classes/config.json";

    public static Logger getLogger() throws Exception {
        if(logger == null){
            initLogger();
        }
        return logger;
    }

    private static void initLogger() throws Exception {
        File logsDirectory = new File(logsPath);
        if (!logsDirectory.exists()) {
            if (!logsDirectory.mkdir()) {
                throw new Exception(String.format("Unable to create dir %s", logsPath));
            }
        }


        String logFullPath = String.format("%s/ua_%s", logsPath, (new SimpleDateFormat("yy_MM_dd___HH_mm")).format(Calendar.getInstance().getTime()));
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] {%2$s} - %4$s -  %5$s%6$s%n");
        logger = Logger.getLogger(UsersManager.class.getName());
        FileHandler fh = new FileHandler(logFullPath);
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }

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
