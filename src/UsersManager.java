import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class UsersManager {
    private Logger logger;
    private MongoConnector mongoConnector;
    private Encryptor encryptor;
    private HashMap<String, String> componentConfig;
    private final static String confPath = Utils.confPath;
    private final static String logsPath = "logs/";
    private final static int MINIMUM_PWD_LEN = 6;
    private Server server;

    public UsersManager(Server server) throws Exception {
        this.server = server;
        logger = Utils.getLogger();
        initConfig();
        mongoConnector = MongoConnector.getInstance();
        encryptor = new Encryptor();
    }

    public Logger getLogger() {
        return this.logger;
    }

    private void initConfig() throws Exception {
        componentConfig = new HashMap<>();
        HashMap<String, Object> tmpConfig = Utils.jsonFileToMap(confPath);
        tmpConfig.forEach((k, v) -> componentConfig.put(k, v.toString()));
        logger.info(String.format("ComponentConfig: %s", componentConfig.toString()));
    }

    private boolean addUser(String userId, String inputPassword, String paypal, String email, String apartmentId) {
        String salt = encryptor.generateSalt();
        while (mongoConnector.isSaltExist(salt)) {
            salt = encryptor.generateSalt();
        }

        String hashedPassword = encryptor.getEncryptedPassword(inputPassword, salt);
        return mongoConnector.insertUser(userId, salt, hashedPassword, paypal, email, apartmentId);
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

    public void update(Object arg) {
        HashMap<String, Object> requestMap = (HashMap<String, Object>) arg;
        String reqType = requestMap.get("type").toString();
        HashMap<String, String> resMap = new HashMap<>(2);
        if (reqType.equals("login")) {
            resMap = handleLoginRequest(requestMap);
        } else if (reqType.equals("set")) {
            resMap = handleSetRequest(requestMap);
        } else if (reqType.equals("change")) {
            resMap = handleChangeRequest(requestMap);
        } else {
            resMap.put("status", "fail");
            resMap.put("error", String.format("Unknown request type: %s", reqType));
        }

        try {
            server.sendManagerResponse(requestMap.get("pendQueueId").toString(), Utils.mapToJson(resMap));
        } catch (Exception e) {
            logger.warning(e.getMessage());
            Arrays.stream(e.getStackTrace()).forEach(st -> logger.warning(st.toString()));
        }
    }

    private HashMap<String, String> handleLoginRequest(HashMap<String, Object> requestMap) {
        HashMap<String, String> resMap = new HashMap<>(1);
        String userId = requestMap.get("userId").toString();
        String password = requestMap.get("password").toString();
        if (!mongoConnector.isUserIdExist(userId)) {
            resMap.put("status", "fail");
            resMap.put("error", "user id doesn't exist");
        } else if (!verifyPassword(userId, password)) {
            resMap.put("status", "fail");
            resMap.put("error", "Authentication failed");
        } else {
            resMap.put("status", "success");
            resMap.put("userId", userId);
            resMap.put("apartmentId", mongoConnector.getAptId(userId));
        }


        return resMap;
    }

    private HashMap<String, String> handleSetRequest(HashMap<String, Object> requestMap) {
        HashMap<String, String> resMap = new HashMap<>(2);
        String userId = requestMap.get("userId").toString();
        String password = requestMap.get("password").toString();
        String paypal = requestMap.get("paypal").toString();
        String email = requestMap.get("email").toString();
        String apartmentId = requestMap.get("apartmentId").toString();
        if (mongoConnector.isUserIdExist(userId)) {
            resMap.put("status", "fail");
            resMap.put("error", "user id already exist");
        } else if (!isValidPasswordForm(password)) {
            resMap.put("status", "fail");
            resMap.put("error", String.format("Invalid password form: must contain lower-case, upper-case and minimum length of %d", MINIMUM_PWD_LEN));
        } else {
            resMap.put("status", addUser(userId, password, paypal, email, apartmentId) ? "success" : "fail");
            resMap.put("userId", userId);
        }

        return resMap;
    }

    private HashMap<String, String> handleChangeRequest(HashMap<String, Object> requestMap) {
        HashMap<String, String> resMap = new HashMap<>(2);
        String userId = requestMap.get("userId").toString();
        String password = requestMap.get("password").toString();
        String newPassword = requestMap.get("newPassword").toString();

        if (!mongoConnector.isUserIdExist(userId)) {
            resMap.put("status", "fail");
            resMap.put("error", "user id not found");
        } else if (!verifyPassword(userId, password)) {
            resMap.put("status", "fail");
            resMap.put("error", "Authentication failed");
        } else if (!isValidPasswordForm(newPassword)) {
            resMap.put("status", "fail");
            resMap.put("error", String.format("Invalid password form: must contain lower-case, upper-case and minimum length of %d", MINIMUM_PWD_LEN));
        } else {
            resMap.put("status", changeUserPassword(userId, password, newPassword) ? "success" : "fail");
            // TODO: in case of exception, put that message in error
        }

        return resMap;
    }
}
//