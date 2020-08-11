import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MessageManager {

    private Server server;
    private Logger logger;
    private WebConnector webConnector;
    private MongoConnector mongoConnector;
    private HashMap<String, String> componentConfig;
    private HashMap<String, HashMap<String, Object>> recordPending;
    private HashMap<String, HashMap<String, String>> waitingApproved;
    private final static String confPath = "resources/config.json";
    private final static String logsPath = "logs/";


    public MessageManager(Server server) throws Exception {
        this.server = server;
        mongoConnector = MongoConnector.getInstance();
        logger = Utils.getLogger();
        componentConfig = new HashMap<>();
        waitingApproved = new HashMap<>();
        recordPending = new HashMap<>();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public ArrayList<Map<String, Object>> getMessages(String userID){
        return mongoConnector.getMessages(userID);
    }
}
