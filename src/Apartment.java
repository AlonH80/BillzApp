import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Apartment {

    private String apartmentId;
    private List<String> userIds;
    private String ownerId;
    private Logger logger;
    private MongoConnector mongoConnector;

    private static String idGenerator() {
        return "";
    }

    public Apartment(String id, String ownerId) throws Exception {
        mongoConnector = MongoConnector.getInstance();
        this.apartmentId = id;
        this.ownerId = ownerId;
        userIds = mongoConnector.getRoommates(id);
        userIds.add(ownerId);
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void addSupplier(Supplier.TYPE type, String supplierId, String openerId) throws Exception {
        mongoConnector.insertSupplier(apartmentId, type.toString(), ownerId);
    }

    public void removeSupplier(Supplier.TYPE type) {
        Map<String, Object> removeMap = new HashMap<>();
        removeMap.put("apartmentId",apartmentId);
        removeMap.put("type", type);
        mongoConnector.remove("billzDB","suppliers",removeMap);

    }

    public void addUser(String userId) {
        userIds.add(userId);
    }

    public void removeUser(String userId) {
        if (userIds.contains(userId)) {
            userIds.remove(userId);
        } else {
            logger.warning(String.format("User %s doesn't exist in this appartment(%s)", userId, apartmentId));
        }
    }

    public void balance() {

    }

    public List<String> getUsers() {
        return userIds;
    }

//    public List<String> getSuppliersIds() {
////        return suppliers.stream().map(Supplier::getSupplierId).collect(Collectors.toList());
//    }

    public String getOwnerId() {
        return ownerId;
    }

//    public Supplier getSupplier(String supplierId) {
////        return suppliers.stream().filter(sup -> sup.getSupplierId().equals(supplierId)).collect(Collectors.toList()).get(0);
//    }

    public String getApartmentId() {
        return apartmentId;
    }

    public Supplier getSupplier(Supplier.TYPE type) throws Exception {
        Map<String, Object> resMap = mongoConnector.getSupplier(apartmentId,type.toString());
        return new Supplier(Supplier.TYPE.valueOf(resMap.get("type").toString().toUpperCase()),resMap.get("apartmentId").toString(),resMap.get("ownerId").toString());
    }
}