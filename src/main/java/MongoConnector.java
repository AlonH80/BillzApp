import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MongoConnector {

    private String address;
    private int port;
    private MongoClient client;
    private Logger logger;
    private String defaultDB = "billzDB";
    private String defaultCollection = "UsersAuth";
    private static MongoConnector instance = null;
    private static HashMap<String, String> componentConfig;
    private final static String confPath = Utils.confPath;

    public static MongoConnector getInstance() throws Exception {
        if (instance == null) {
            instance = new MongoConnector();
            instance.setDefaultDB(componentConfig.get("mongoAppDB"));
            instance.setDefaultCollection(componentConfig.get("mongoAppCollection"));
        }
        return instance;
    }


    public MongoConnector() throws Exception {
        logger = Utils.getLogger();
        initConfig();
        this.address = componentConfig.get("mongoAddress");
        initClient();
    }

    public MongoConnector(String address, int port) {
        this.address = address;
        this.port = port;
        initClient();
    }

    private void initClient() {
        client = MongoClients.create(address);
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setDefaultDB(String db) {
        defaultDB = db;
    }

    public void setDefaultCollection(String collection) {
        defaultCollection = collection;
    }

    private void insert(String database, String collection, Map<String, Object> insertMap) {
        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        insertMap.put("date", Utils.generateDateTimeStamp());
        InsertOneResult res = mongoCollection.insertOne(new Document(insertMap));
        logger.info(String.format("inserted info regard %s to %s.%s", Utils.mapToJson(insertMap), database, collection));
    }

    public void remove(String database, String collection, Map<String, Object> removeMap) {
        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        mongoCollection.deleteOne(new Document(removeMap));
        logger.info(String.format("removed info regard %s to %s.%s", Utils.mapToJson(removeMap), database, collection));
    }

    private void update(String database, String collection, Map<String, String> queryMap, Map<String, Object> updateMap) {
        updateMap.put("date", Utils.generateDateTimeStamp());
        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        mongoCollection.findOneAndReplace(mapToBson(queryMap), new Document(updateMap));
        logger.info(String.format("Updated info regard %s in %s.%s", queryMap.get("userID"), database, collection));
    }

    public ArrayList<Map<String, Object>> find(String database, String collection, Map<String, String> findMap) {
        ArrayList<Map<String, Object>> resMap = new ArrayList<>();
        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        FindIterable<Document> res = mongoCollection.find(mapToBson(findMap));
        res.forEach(document -> {
            document.remove("_id");
            HashMap<String, Object> docMap = new LinkedHashMap<>(document);
            resMap.add(docMap);
            //document.forEach((k, v)-> docMap.put(k, v));
        });
        logger.info(String.format("find in %s.%s, query: %s", database, collection, Utils.mapToJson(findMap)));

        return resMap;
    }

    private BsonDocument mapToBson(Map<String, ?> map) {
        BsonDocument bsonDocument = new BsonDocument();
        map.forEach((k, v) -> bsonDocument.append(k, new BsonString(v.toString())));
        return bsonDocument;
    }

    public boolean insertUser(String userID, String salt, String hashedPassword, String paypal, String email, String apartmentId) {
        boolean userInserted = false;
        if (!isUserIdExist(userID)) {
            LinkedTreeMap<String, Object> newUserMap = new LinkedTreeMap<>();
            newUserMap.put("userID", userID);
            newUserMap.put("salt", salt);
            newUserMap.put("password", hashedPassword);
            newUserMap.put("paypal", paypal);
            newUserMap.put("email", email);
            if (apartmentId == null) {
                newUserMap.put("apartmentId", "0");
            } else {
                newUserMap.put("apartmentId", apartmentId);
            }
            insert(defaultDB, "UsersAuth", newUserMap);
            userInserted = true;
        } else {
            logger.warning(String.format("User id %s already exist", userID));
        }
        return userInserted;
    }

    public boolean checkPasswordMatch(String userID, String hashedPassword) {
        HashMap<String, String> queryMap = new HashMap<>(1);
        queryMap.put("userID", userID);
        String passwordInDB = find(defaultDB, "UsersAuth", queryMap).get(0).get("password").toString();
        boolean passwordMatch = passwordInDB.equals(hashedPassword);
        if (!passwordMatch) {
            logger.warning(String.format("User %s entered incorrect password", userID));
        }
        return passwordMatch;
    }

    public String getUserSalt(String userID) {
        HashMap<String, String> queryMap = new HashMap<>(1);
        queryMap.put("userID", userID);
        String userSalt = find(defaultDB, "UsersAuth", queryMap).get(0).get("salt").toString();

        return userSalt;
    }

    public boolean updateUserPassword(String userID, String salt, String hashedPassword) {
        try {
            HashMap<String, String> queryMap = new HashMap<>(1);
            queryMap.put("userID", userID);
            LinkedTreeMap<String, Object> updateMap = new LinkedTreeMap<>();
            updateMap.put("userID", userID);
            updateMap.put("salt", salt);
            updateMap.put("password", hashedPassword);
            update(defaultDB, "UsersAuth", queryMap, updateMap);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private HashSet<String> getAllUsersIds() {
        ArrayList<Map<String, Object>> usersAuthMap = find(defaultDB, "UsersAuth", new HashMap<>());
        HashSet<String> usersIds = new HashSet<>(usersAuthMap.size());
        usersAuthMap.forEach(m -> usersIds.add(m.get("userID").toString()));

        return usersIds;
    }

    public boolean isUserIdExist(String userId) {
        return getAllUsersIds().contains(userId);
    }

    private HashSet<String> getAllSalts() {
        ArrayList<Map<String, Object>> usersAuthMap = find(defaultDB, "UsersAuth", new HashMap<>());
        HashSet<String> salts = new HashSet<>(usersAuthMap.size());
        usersAuthMap.forEach(m -> salts.add(m.get("salt").toString()));

        return salts;
    }

    public Map<String, Object> getUser(String userId) {
        HashMap<String, String> userMap = new HashMap<>(1);
        userMap.put("id", userId);
        Map<String, Object> resMap = find("billzDB", "users", userMap).get(0);
        logger.info(String.format("user map: %s", (new Gson()).toJson(resMap)));
        return resMap;
    }

    public void recordTransaction(String userIdFrom, String userIdTo, Double amount) {
        Map<String, Object> recordMap = new LinkedHashMap<>(3);
        recordMap.put("userIdFrom", userIdFrom);
        recordMap.put("userIdTo", userIdTo);
        recordMap.put("amount", amount);
        insert("billzDB", "transactions", recordMap);
    }

    public boolean isSaltExist(String salt) {
        return getAllSalts().contains(salt);
    }

    private void initConfig() throws Exception {
        componentConfig = Utils.loadConfigs();
    }

    public ArrayList<Map<String, Object>> getMessages(String userId) {
        HashMap<String, String> userMap = new HashMap<>(1);
        userMap.put("id", userId);
        ArrayList<Map<String, Object>> resMap = find("billzDB", "messages", userMap);
        logger.info(String.format("user map: %s", (new Gson()).toJson(resMap)));
        return resMap;
    }

    public void insertMessage(String userID, String salt, String hashedPassword) {
        LinkedTreeMap<String, Object> newUserMap = new LinkedTreeMap<>();
        newUserMap.put("userID", userID);
        newUserMap.put("salt", salt);
        newUserMap.put("password", hashedPassword);
        insert(defaultDB, "UsersAuth", newUserMap);
    }

    public String generateIdForApartment() {
        return generateId("billzDB", "apartments");
    }

    public String generateIdForSupplier() {
        return generateId("billzDB", "suppliers");
    }

    public String generateId(String database, String collection) {
        List<Map<String, Object>> collectionList = find(database, collection, new HashMap<>());
        Set<String> ids = collectionList.stream().map(ent -> ent.get("id").toString()).collect(Collectors.toSet());
        String generatedId = randomlyGenerateId();
        int numberOfTries = 1;
        while (ids.contains(generatedId)) {
            generatedId = randomlyGenerateId();
            ++numberOfTries;
        }

        logger.info(String.format("Number of random generate: %d", numberOfTries));
        return generatedId;
    }

    private String randomlyGenerateId() {
        int randomGeneratedSize = 6;
        int randomType, randomChar;
        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());
        StringBuilder generatedId = new StringBuilder(randomGeneratedSize);
        for (int i = 0; i < randomGeneratedSize; i++) {
            randomType = Math.abs(rand.nextInt());
            randomChar = Math.abs(rand.nextInt());
            randomType = (randomType % 3) + 1;
            switch (randomType) {
                case 1:
                    generatedId.append((char) ((int) '0' + randomChar % 10));
                    break;
                case 2:
                    generatedId.append((char) ((int) 'a' + randomChar % 26));
                    break;
                case 3:
                    generatedId.append((char) ((int) 'A' + randomChar % 26));
                    break;
            }
        }

        return generatedId.toString();
    }

    public void insertApartment(String apartmentId, List<String> usersIds, String openerId) {
        LinkedTreeMap<String, Object> insertMap = new LinkedTreeMap<>();
        insertMap.put("id", apartmentId);
        insertMap.put("usersIds", usersIds);
        insertMap.put("ownerId", openerId);
        insert("billzDB", "apartments", insertMap);
    }

    public void updateApartmentUsers(String apartId, List<String> users) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();
        queryMap.put("id", apartId);
        Map<String, Object> updateMap = find("billzDB", "apartments", queryMap).get(0);
        updateMap.replace("usersIds", users);
        update("billzDB", "apartments", queryMap, updateMap);
    }

    public void updateApartmentForUsersId(String apartId, String user) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();
        queryMap.put("userID", user);
        Map<String, Object> updateMap = find("billzDB", "UsersAuth", queryMap).get(0);
        updateMap.replace("apartmentId", apartId);
        update("billzDB", "UsersAuth", queryMap, updateMap);
    }

    public void updateApartmentSuppliers(String apartId, List<String> suppliers) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();
        queryMap.put("id", apartId);
        Map<String, Object> updateMap = find("billzDB", "apartments", queryMap).get(0);
        updateMap.replace("suppliersIds", suppliers);
        update("billzDB", "apartments", queryMap, updateMap);
    }

    public void insertSupplier(String belongToApartmentId, String type, String ownerId) {
        LinkedHashMap<String, Object> insertMap = new LinkedHashMap<>();
        insertMap.put("apartmentId", belongToApartmentId);
        insertMap.put("type", type);
        insertMap.put("ownerId", ownerId);
        insertMap.put("balance", "0");
        insert("billzDB", "suppliers", insertMap);
    }

    public void insertSupplierParts(String belongToApartmentId, String type, Map<String, Object> parts) {
        parts.forEach((k, v) -> {
            LinkedHashMap<String, Object> insertMap = new LinkedHashMap<>();
            insertMap.put("apartmentId", belongToApartmentId);
            insertMap.put("type", type);
            insertMap.put("userId", k);
            insertMap.put("part", v);
            insert("billzDB", "suppliersPart", insertMap);
        });
    }

    public ArrayList<Map<String, Object>> getSupplierParts(String belongToApartmentId, String type) {
        LinkedHashMap<String, String> findMap = new LinkedHashMap<>();
        findMap.put("apartmentId", belongToApartmentId);
        findMap.put("type", type);
        return find("billzDB", "suppliersPart", findMap);
    }

    public void insertSupplierBalance(String belongToApartmentId, String type, Map<String, Object> balance) {
        balance.forEach((k, v) -> {
            LinkedHashMap<String, Object> insertMap = new LinkedHashMap<>();
            insertMap.put("apartmentId", belongToApartmentId);
            insertMap.put("type", type);
            insertMap.put("userId", k);
            insertMap.put("balance", v);
            insert("billzDB", "suppliersBalance", insertMap);
        });
    }

    public void insertSupplierBalances(String type, String apartmentId, Map<String, Double> usersBalances) {
        LinkedHashMap<String, Object> insertMap = new LinkedHashMap<>();
        insertMap.put("type", type);
        insertMap.put("apartmentId", apartmentId);
        insertMap.put("balances", usersBalances);
        insert("billzDB", "suppliersBalance", insertMap);
    }

    public void updateSupplierBalances(String apartmentId, String type, String newSupplierBalance) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();
        queryMap.put("apartmentId", apartmentId);
        queryMap.put("type", type);
        Map<String, Object> updateMap = find("billzDB", "supplier", queryMap).get(0);
        updateMap.replace("balances", newSupplierBalance);
        update("billzDB", "supplier", queryMap, updateMap);
    }

    public void updateUserSupplierBalance(String apartmentId, String type, String newUserBalance) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();
        queryMap.put("apartmentId", apartmentId);
        queryMap.put("type", type);
        Map<String, Object> updateMap = find("billzDB", "suppliersBalance", queryMap).get(0);
        updateMap.replace("balances", newUserBalance);
        update("billzDB", "suppliersBalance", queryMap, updateMap);
    }

    public Map<String, Object> getApartment(String apartmentId) {
        HashMap<String, String> aptMap = new HashMap<>(1);
        aptMap.put("id", apartmentId);
        Map<String, Object> resMap = find("billzDB", "apartments", aptMap).get(0);
        logger.info(String.format("apartment map: %s", (new Gson()).toJson(resMap)));
        return resMap;
    }

    public Map<String, Object> getSupplier(String apartmentId, String type) {
        HashMap<String, String> supplierMap = new HashMap<>(1);
        supplierMap.put("id", apartmentId);
        supplierMap.put("type", type);
        Map<String, Object> resMap = find("billzDB", "suppliers", supplierMap).get(0);
        logger.info(String.format("apartment map: %s", (new Gson()).toJson(resMap)));
        return resMap;
    }

    public String getAptId(String userId) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();
        queryMap.put("userID", userId);
        Map<String, Object> resMap = find("billzDB", "UsersAuth", queryMap).get(0);
        return resMap.get("apartmentId").toString();
    }

    public List<String> getRoommates(String apartmentId) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();
        queryMap.put("apartmentId", apartmentId);
        ArrayList<Map<String, Object>> resMap = find("billzDB", "apartments", queryMap);
        if(resMap.isEmpty())
            return new ArrayList<>();
        else
            return (List<String>) resMap.get(0).get("usersIds");
    }

    public List<Map<String, Object>> getSuppliers(String apartmentId) {
        HashMap<String, String> supplierMap = new HashMap<>(1);
        supplierMap.put("apartmentId", apartmentId);
        List<Map<String, Object>> resLst = find("billzDB", "suppliers", supplierMap);
        logger.info(String.format("apartment map: %s", (new Gson()).toJson(resLst)));
        return resLst;
    }

    public void addBill(String apartmentId, String dDay, String amount, String billType, String userId) {
        LinkedHashMap<String, Object> insertMap = new LinkedHashMap<>();
        insertMap.put("apartmentId", apartmentId);
        insertMap.put("dDay", dDay);
        insertMap.put("amount", amount);
        insertMap.put("billType", billType);
        insertMap.put("owner", userId);
        insertMap.put("status", "UNPAID");
        insert("billzDB", "bills", insertMap);
    }

    public List getBills(String apartmentId) {
        HashMap<String, String> billsMap = new HashMap<>(1);
        billsMap.put("apartmentId", apartmentId);
        List<Map<String, Object>> resLst = find("billzDB", "bills", billsMap);
        logger.info(String.format("apartment map: %s", (new Gson()).toJson(resLst)));
        return resLst;
    }

    public void updateSetting(String userId, String setting, String value) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();
        queryMap.put("userID", userId);
        Map<String, Object> updateMap = find("billzDB", "UsersAuth", queryMap).get(0);
        updateMap.replace(setting, value);
        update("billzDB", "UsersAuth", queryMap, updateMap);
    }
}