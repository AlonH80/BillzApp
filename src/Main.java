import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

    public static void main(String[] args) {
        try {
            UsersManager usersManager = new UsersManager();
//            MongoConnector mongoConnector = new MongoConnector();
//            ArrayList<Map<String, Object>> findMap = mongoConnector.find("billzDB", "UsersAuth", new HashMap<>());
//            findMap.forEach(m-> System.out.println(m.get("userID").toString()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private static Logger initLogger() throws Exception {
//        File logsDirectory = new File("logs/");
//        if (! logsDirectory.exists()) {
//            logsDirectory.mkdir();
//        }
//
//        String logFullPath = String.format("%s/ua_%s", "logs", (new SimpleDateFormat("yy_MM_dd___HH_mm")).format(Calendar.getInstance().getTime()));
//        System.setProperty("java.util.logging.SimpleFormatter.format",
//                "[%1$tF %1$tT] {%2$s} - %4$s -  %5$s%6$s%n");
//        Logger logger = Logger.getLogger("serverTest");
//        FileHandler fh = new FileHandler(logFullPath);
//        logger.addHandler(fh);
//        SimpleFormatter formatter = new SimpleFormatter();
//        fh.setFormatter(formatter);
//
//        return logger;
//    }
}
