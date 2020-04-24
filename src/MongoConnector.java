import com.google.gson.internal.LinkedTreeMap;
import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;

import java.util.*;
import java.util.logging.Logger;

public class MongoConnector {

    private  String address;
    private int port;
    private MongoClient client;
    private Logger logger;
    private String defaultDB = "billzDB"; // TODO: get from config file
    private String defaultCollection = "UsersAuth"; // TODO: get from config file

    public MongoConnector(){
        this.address = "localhost";
        this.port = 27017;
        initClient();
    }

    public MongoConnector(String address, int port){
        this.address = address;
        this.port = port;
        initClient();
    }

    private void initClient() {
        client = MongoClients.create(String.format("mongodb://%s:%d", address, port));
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    private void insert(String database, String collection, Map<String, Object> insertMap) {
        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        insertMap.put("date", Utils.generateDateTimeStamp());
        InsertOneResult res = mongoCollection.insertOne(new Document(insertMap));
        logger.info(String.format("inserted info regard %s to %s.%s", insertMap.get("userID"), database, collection));
    }

    private void update(String database, String collection, Map<String, Object> queryMap, Map<String, Object> updateMap) {
        updateMap.put("date", Utils.generateDateTimeStamp());
        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        mongoCollection.findOneAndReplace(mapToBson(queryMap), new Document(updateMap));
        logger.info(String.format("Updated info regard %s in %s.%s", queryMap.get("userID"), database, collection));
    }

    private ArrayList<Map<String, Object>> find(String database, String collection, Map<String, String> findMap) {
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
        logger.info(String.format("find info regard %s in %s.%s", resMap.get(0).get("userID"), database, collection));

        return resMap;
    }

    private BsonDocument mapToBson(Map<String, ?> map) {
        BsonDocument bsonDocument = new BsonDocument();
        map.forEach((k, v) -> bsonDocument.append(k, new BsonString(v.toString())));
        return bsonDocument;
    }

    public boolean insertUser(String userID, String salt, String hashedPassword) {
        // verifyUniqUserId;
        LinkedTreeMap<String, Object> newUserMap = new LinkedTreeMap<>();
        newUserMap.put("userID", userID);
        newUserMap.put("salt", salt);
        newUserMap.put("password", hashedPassword);
        insert(defaultDB,defaultCollection, newUserMap);
        return true;
    }

    public boolean checkPasswordMatch(String userID, String hashedPassword) {
        HashMap<String, String> queryMap = new HashMap<>(1);
        queryMap.put("userID", userID);
        String passwordInDB = find(defaultDB,defaultCollection, queryMap).get(0).get("password").toString();
        boolean passwordMatch = passwordInDB.equals(hashedPassword);
        if (!passwordMatch) {
            logger.warning(String.format("User %s entered incorrect password", userID));
        }
        return passwordMatch;
    }

    public String getUserSalt(String userID) {
        HashMap<String, String> queryMap = new HashMap<>(1);
        queryMap.put("userID", userID);
        String userSalt = find(defaultDB,defaultCollection, queryMap).get(0).get("salt").toString();

        return userSalt;
    }

    public boolean updateUserPassword(String userID, String salt, String hashedPassword) {
        try {
            HashMap<String, Object> queryMap = new HashMap<>(1);
            queryMap.put("userID", userID);
            LinkedTreeMap<String, Object> updateMap = new LinkedTreeMap<>();
            updateMap.put("userID", userID);
            updateMap.put("salt", salt);
            updateMap.put("password", hashedPassword);
            update(defaultDB, defaultCollection, queryMap, updateMap);
            return true;
        }
        catch (Exception e){
            return false;
        }

    }
}

