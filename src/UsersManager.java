import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class UsersManager implements Observer {

    private UsersAuthServer usersAuthServer;
    private Logger logger;
    private MongoConnector mongoConnector;
    private Encryptor encryptor;
    private HashMap<String, String> componentConfig;
//    private HashMap<String, HashMap<String, Object>> recordPending;
//    private HashMap<String, HashMap<String, String>> waitingApproved;
    private final static String confPath = "resources/config.json";
    private final static String logsPath = "logs/";

    public UsersManager() throws Exception {
        initLogger();
        initConfig();
        usersAuthServer = new UsersAuthServer();
        usersAuthServer.setLogger(logger);
        usersAuthServer.addObserver(this);
        mongoConnector = new MongoConnector(componentConfig.get("mongoAddress"), Integer.parseInt(componentConfig.get("mongoPort")));
        mongoConnector.setLogger(logger);
        encryptor = new Encryptor();
    }

    private void initLogger() throws Exception {
        File logsDirectory = new File(logsPath);
        if (! logsDirectory.exists()) {
            if (!logsDirectory.mkdir()){
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

    private void initConfig() throws Exception {
        componentConfig = new HashMap<>();
        HashMap<String, Object> tmpConfig = Utils.jsonFileToMap(confPath);
        tmpConfig.forEach((k,v)->componentConfig.put(k, v.toString()));
        logger.info(String.format("ComponentConfig: %s", componentConfig.toString()));
    }

    private boolean addUser(String userId, String inputPassword) {
        String salt = encryptor.generateSalt();
        String hashedPassword = encryptor.getEncryptedPassword(inputPassword, salt);
        return mongoConnector.insertUser(userId, salt, hashedPassword);
    }

    private boolean changeUserPassword(String userId, String oldPassword, String newPassword) {
        boolean passwordChanged = false;
        if (verifyPassword(userId, oldPassword)) {
            String salt = encryptor.generateSalt();
            String hashedPassword = encryptor.getEncryptedPassword(newPassword, salt);
            passwordChanged = mongoConnector.updateUserPassword(userId, salt, hashedPassword);
        }

        return passwordChanged;
    }

    public boolean verifyPassword(String userId, String inputPassword) {
        String salt = mongoConnector.getUserSalt(userId);
        String hashedPassword = encryptor.getEncryptedPassword(inputPassword, salt);
        return mongoConnector.checkPasswordMatch(userId, hashedPassword);
    }

    @Override
    public void update(Observable o, Object arg) {
        HashMap<String, Object> requestMap = (HashMap<String, Object>)arg;
        String reqType = requestMap.get("type").toString();
        String userId = requestMap.get("userId").toString();
        String password = requestMap.get("password").toString();
        HashMap<String, String> resMap = new HashMap<>(1);
        resMap.put("status", "fail");
        boolean res = false;

        if (reqType.equals("login")) {
            res = verifyPassword(userId, password);
        }
        else if (reqType.equals("set")) {
            res = addUser(userId, password);
        }
        else if (reqType.equals("change")) {
            res = changeUserPassword(userId, password, requestMap.get("newPassword").toString());
        }

        if (res) {
            resMap.replace("status", "success");
        }

        try {
            usersAuthServer.sendManagerRepsonse(requestMap.get("pendQueueId").toString(), Utils.mapToJson(resMap));
        }
        catch (Exception e){
            logger.warning(e.getMessage());
            Arrays.stream(e.getStackTrace()).forEach(st->logger.warning(st.toString()));
        }
    }
}
