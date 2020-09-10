import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ApartsManager {

    private Map<String, Apartment> apartments;
    private HashMap<String, String> configuration;
    private Logger logger;
    private MongoConnector mongoConnector;

    public ApartsManager() throws Exception {
        configuration = Utils.loadConfigs();
        logger = Utils.getLogger();
        apartments = new LinkedHashMap<>();
        mongoConnector = MongoConnector.getInstance();
    }

    public String createApartment(String creatorId) throws Exception {
        String apartId = mongoConnector.generateIdForApartment();
        Apartment newApart = new Apartment(apartId, creatorId);
        apartments.put(apartId, newApart);
        mongoConnector.insertApartment(apartId, newApart.getUsers(), newApart.getOwnerId());
        //addUserToApartment(apartId,creatorId);
        mongoConnector.updateApartmentForUsersId(apartId, creatorId);
        return apartId;
    }

    public String addSupplierToApartment(String apartmentId, String ownerId, Supplier.TYPE type, Map<String,Object> partsMap) throws Exception {
//        String supplierId = mongoConnector.generateIdForSupplier();
        Apartment currApart = getApartment(apartmentId);
        currApart.addSupplier(type, ownerId);
        mongoConnector.insertSupplierParts(apartmentId, type.toString(), partsMap);
//        mongoConnector.insertSupplierBalances(supplierId, currSupplier.getBalances());
        return ownerId;
    }

    private Apartment getApartment(String apartmentId) throws Exception {
        Map<String, Object> apartment = mongoConnector.getApartment(apartmentId);
        return new Apartment(apartment.get("id").toString(), apartment.get("ownerId").toString());
    }

    public void leaveApartment(String userId){
        mongoConnector.leaveApartment(userId);
    }


    public void addUserToApartment(String apartmentId, String userId) throws Exception {
        Apartment currApartment = getApartment(apartmentId);
        currApartment.addUser(userId);
        mongoConnector.updateApartmentUsers(apartmentId, currApartment.getUsers());
        mongoConnector.updateApartmentForUsersId(apartmentId, userId);
    }

    public void addUserToBill(String apartmentId, String userId, String supplierId) throws Exception {
        Apartment currApartment = getApartment(apartmentId);
//        currApartment.getSupplier(supplierId).addPayer(userId);
        mongoConnector.updateApartmentUsers(apartmentId, currApartment.getUsers());
    }

    public void addNewBillToApartment(String apartmentId, String supplierId, Double amount) throws Exception {
        Apartment currApart = getApartment(apartmentId);
        Supplier currSupplier = currApart.getSupplier(Supplier.TYPE.valueOf(supplierId));
        currSupplier.addBill(amount);
        currSupplier.updateBalances(amount.toString());
        //currSupplier.getBalances().forEach((userId, newUserBalance) -> mongoConnector.updateUserSupplierBalance(userId, apartmentId,supplierId, newUserBalance.toString()));
    }

    public List<String> getRoommates(String apartmentId, String userId) throws Exception {
        Apartment currApart = getApartment(apartmentId);
        return currApart.getUsers();
    }

    public List<Map<String, Object>> getSuppliers(String apartmentId) throws Exception {
        Apartment currApart = getApartment(apartmentId);
        return currApart.getSuppliers(apartmentId);

    }

    public void addBill(String apartmentId, String dDay, String amount, String billType, String userId) throws Exception {
        Apartment currApart = getApartment(apartmentId);
        currApart.addBill(dDay,amount, billType, userId);
    }

    public List getBills(String apartmentId) {
        return mongoConnector.getBills(apartmentId);
    }

    public void addRoommate(String apartmentId, String userId) throws Exception {
        addUserToApartment(apartmentId, userId);
    }

    public Map<String, String> getApartmentBalances(String apartmentId) {
        List<Map<String, Object>> apartBalancesRaw = mongoConnector.getApartmentBalances(apartmentId);
        HashMap<String, String> usersBalances = new HashMap<>();
        apartBalancesRaw.forEach(bal-> {
            if (!usersBalances.containsKey(bal.get("userId").toString())) {
                usersBalances.put(bal.get("userId").toString(), "0");
            }
            String uid = bal.get("userId").toString();
            usersBalances.put(uid, String.valueOf(Double.parseDouble(usersBalances.get(uid)) + Double.parseDouble(bal.get("balance").toString())));
        });
        return usersBalances;
    }
}