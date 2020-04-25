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
    private final static String confPath = "resources/config.json";
    private final static String logsPath = "logs/";
    private final static int MINIMUM_PWD_LEN = 6;

    public UsersManager() throws Exception {
        initLogger();
        initConfig();
        usersAuthServer = new UsersAuthServer(Integer.parseInt(componentConfig.get("UsersAuthServerPort")));
        usersAuthServer.setLogger(logger);
        usersAuthServer.addObserver(this);
        mongoConnector = new MongoConnector(componentConfig.get("mongoAddress"), Integer.parseInt(componentConfig.get("mongoPort")));
        mongoConnector.setLogger(logger);
        mongoConnector.setDefaultDB(componentConfig.get("mongoAppDB"));
        mongoConnector.setDefaultCollection(componentConfig.get("mongoAppCollection"));
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
        while(mongoConnector.isSaltExist(salt)) {
            salt = encryptor.generateSalt();
        }

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

    private boolean isValidPasswordForm(String password) {
        return Utils.containsLowerCase(password) && Utils.containsUpperCase(password) && password.length() >= MINIMUM_PWD_LEN;
    }

    @Override
    public void update(Observable o, Object arg) {
        HashMap<String, Object> requestMap = (HashMap<String, Object>)arg;
        String reqType = requestMap.get("type").toString();
        HashMap<String, String> resMap = new HashMap<>(2);

        if (reqType.equals("login")) {
            resMap = handleLoginRequest(requestMap);
        }
        else if (reqType.equals("set")) {
            resMap = handleSetRequest(requestMap);
        }
        else if (reqType.equals("change")) {
            resMap = handleChangeRequest(requestMap);
        }
        else {
            resMap.put("status", "fail");
            resMap.put("error", String.format("Unknown request type: %s", reqType));
        }

        try {
            usersAuthServer.sendManagerRepsonse(requestMap.get("pendQueueId").toString(), Utils.mapToJson(resMap));
        }
        catch (Exception e){
            logger.warning(e.getMessage());
            Arrays.stream(e.getStackTrace()).forEach(st->logger.warning(st.toString()));
        }
    }

    private HashMap<String, String> handleLoginRequest(HashMap<String, Object> requestMap) {
        HashMap<String, String> resMap = new HashMap<>(1);
        String userId = requestMap.get("userId").toString();
        String password = requestMap.get("password").toString();
        if (!mongoConnector.isUserIdExist(userId)){
            resMap.put("status", "fail");
            resMap.put("error", "user id doesn't exist");
        }
        else if (!verifyPassword(userId, password)){
            resMap.put("status", "fail");
            resMap.put("error", "Authentication failed");
        }
        else {
            resMap.put("status", "success");
        }


        return resMap;
    }

    private HashMap<String, String> handleSetRequest(HashMap<String, Object> requestMap) {
        HashMap<String, String> resMap = new HashMap<>(2);
        String userId = requestMap.get("userId").toString();
        String password = requestMap.get("password").toString();

        if (mongoConnector.isUserIdExist(userId)){
            resMap.put("status", "fail");
            resMap.put("error", "user id already exist");
        }
        else if(!isValidPasswordForm(password)) {
            resMap.put("status", "fail");
            resMap.put("error", String.format("Invalid password form: must contain lower-case, upper-case and minimum length of %d", MINIMUM_PWD_LEN));
        }
        else {
            resMap.put("status", addUser(userId, password)? "success" : "fail");
        }

        return resMap;
    }

    private HashMap<String, String> handleChangeRequest(HashMap<String, Object> requestMap) {
        HashMap<String, String> resMap = new HashMap<>(2);
        String userId = requestMap.get("userId").toString();
        String password = requestMap.get("password").toString();
        String newPassword = requestMap.get("newPassword").toString();

        if (!mongoConnector.isUserIdExist(userId)){
            resMap.put("status", "fail");
            resMap.put("error", "user id not found");
        }
        else if (!verifyPassword(userId, password)) {
            resMap.put("status", "fail");
            resMap.put("error", "Authentication failed");
        }
        else if (!isValidPasswordForm(newPassword)){
            resMap.put("status", "fail");
            resMap.put("error", String.format("Invalid password form: must contain lower-case, upper-case and minimum length of %d", MINIMUM_PWD_LEN));
        }
        else {
            resMap.put("status", changeUserPassword(userId, password, newPassword)? "success" : "fail");
        }

        return resMap;
    }
}
//