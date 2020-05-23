import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.mongodb.client.*;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MongoConnector {

    private  String address = "localhost";
    private int port = 27017;
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

    public ArrayList<Map<String, Object>> find(String database, String collection, Map<String, Object> findMap) {
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
        insertMap.put("date", Utils.generateDateTimeStamp());
        InsertOneResult res = mongoCollection.insertOne(new Document(insertMap));
        logger.info(String.format("inserted to %s.%s: %s", database, collection, (new Gson()).toJson(insertMap)));
    }

    private void update(String database, String collection, Map<String, Object> queryMap, Map<String, Object> updateMap) {
        updateMap.put("date", Utils.generateDateTimeStamp());
        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        mongoCollection.findOneAndReplace(mapToBson(queryMap), new Document(updateMap));
        logger.info(String.format("Updated info regard %s in %s.%s", queryMap.get("id"), database, collection));
    }

    private BsonDocument mapToBson(Map<String, ?> map) {
        BsonDocument bsonDocument = new BsonDocument();
        map.forEach((k, v) -> bsonDocument.append(k, new BsonString(v.toString())));
        return bsonDocument;
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
        for(int i = 0; i < randomGeneratedSize; i++) {
            randomType = Math.abs(rand.nextInt());
            randomChar = Math.abs(rand.nextInt());
            randomType = (randomType % 3) + 1;
            switch (randomType){
                case 1: generatedId.append((char)((int)'0' + randomChar % 10));
                        break;
                case 2: generatedId.append((char)((int)'a' + randomChar % 26));
                        break;
                case 3: generatedId.append((char)((int)'A' + randomChar % 26));
                        break;
            }
        }

        return generatedId.toString();
    }

    public void insertApartment(String apartmentId, List<String> usersIds, List<String> suppliersIds, String openerId) {
        LinkedTreeMap<String, Object> insertMap = new LinkedTreeMap<>();
        insertMap.put("id", apartmentId);
        insertMap.put("usersIds", usersIds);
        insertMap.put("suppliersIds", suppliersIds);
        insertMap.put("openerId", openerId);
        insert("billzDB", "apartments", insertMap);
    }

    public void updateApartmentUsers(String apartId, List<String> users) {
        LinkedHashMap<String, Object> queryMap = new LinkedHashMap<>();
        queryMap.put("id", apartId);
        Map<String, Object> updateMap = find("billzDB", "apartments", queryMap).get(0);
        updateMap.replace("usersIds", users);
        update("billzDB", "apartments", queryMap, updateMap);
    }

    public void updateApartmentSuppliers(String apartId, List<String> suppliers) {
        LinkedHashMap<String, Object> queryMap = new LinkedHashMap<>();
        queryMap.put("id", apartId);
        Map<String, Object> updateMap = find("billzDB", "apartments", queryMap).get(0);
        updateMap.replace("suppliersIds", suppliers);
        update("billzDB", "apartments", queryMap, updateMap);
    }

    public void insertSupplier(String supplierId, String belongToApartmentId, Supplier.TYPE supplierType, String ownerId) {
        LinkedHashMap<String, Object> insertMap = new LinkedHashMap<>();
        insertMap.put("id", supplierId);
        insertMap.put("apartmentId", belongToApartmentId);
        insertMap.put("type", supplierType.toString());
        insertMap.put("ownwerId", ownerId);
        insert("billzDB", "suppliers", insertMap);
    }

    public void insertSupplierBalances(String supplierId, Map<String, Double> usersBalances) {
        LinkedHashMap<String, Object> insertMap = new LinkedHashMap<>();
        insertMap.put("id", supplierId);
        insertMap.put("balances", usersBalances);
        insert("billzDB", "suppliersBalance", insertMap);
    }

    public void updateSupplierBalances(String supplierId, Map<String, Double> usersBalances) {
        LinkedHashMap<String, Object> queryMap = new LinkedHashMap<>();
        queryMap.put("id", supplierId);
        Map<String, Object> updateMap = find("billzDB", "suppliersBalance", queryMap).get(0);
        updateMap.replace("balances", usersBalances);
        update("billzDB", "suppliersBalance", queryMap, updateMap);
    }


}
