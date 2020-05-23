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

    public static HashMap<String,Object> jsonFileToMap(String filePath) throws Exception {
        return (new Gson()).fromJson(new JsonReader(new FileReader(filePath)), HashMap.class);
    }

    public static Logger initLogger(String logsPath, String loggerName) throws Exception {
        File logsDirectory = new File(logsPath);
        if (! logsDirectory.exists()) {
            if (!logsDirectory.mkdir()){
                throw new Exception(String.format("Unable to create dir %s", logsPath));
            }
        }

        String logFullPath = String.format("%s/am_%s", logsPath, (new SimpleDateFormat("yy_MM_dd___HH_mm")).format(Calendar.getInstance().getTime()));
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] {%2$s} - %4$s -  %5$s%6$s%n");
        Logger logger = Logger.getLogger(loggerName);
        FileHandler fh = new FileHandler(logFullPath);
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);

        return logger;
    }

}
