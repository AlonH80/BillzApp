import com.google.gson.Gson;
import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class MongoConnector {

    private  String address = "localhost"; // TODO: add to config
    private int port = 27017    ; // TODO: add to config
    private MongoClient client;
    private Logger logger;

    public MongoConnector(){
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
        logger.info(String.format("find in %s.%s: %s", database, collection, (new Gson()).toJson(resMap)));

        return resMap;
    }

    public void insert(String database, String collection, Map<String, Object> insertMap) {
        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        insertMap.put("date", Calendar.getInstance().getTime().toString()); //TODO: set proper date format
        InsertOneResult res = mongoCollection.insertOne(new Document(insertMap));
        logger.info(String.format("inserted to %s.%s: %s", database, collection, (new Gson()).toJson(insertMap)));
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

    private BsonDocument mapToBson(Map<String, String> map) {
        BsonDocument bsonDocument = new BsonDocument();
        map.forEach((k, v) -> bsonDocument.append(k, new BsonString(v)));
        return bsonDocument;
    }
}
