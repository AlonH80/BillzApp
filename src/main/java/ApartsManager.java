import com.google.gson.Gson;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ApartsManager {

    private Map<String, Apartment> apartments;
    private final static String configPath = "target/resources/config.json";
    private Map<String, Object> configuration;
    private Logger logger;
    private MongoConnector mongoConnector;

    public ApartsManager() throws Exception {
        configuration = Utils.jsonFileToMap(configPath);
        logger = Utils.getLogger();
        apartments = new LinkedHashMap<>();
        mongoConnector = MongoConnector.getInstance();
    }

    public String createApartment(String creatorId) throws Exception {
        String apartId = mongoConnector.generateIdForApartment();
        Apartment newApart = new Apartment(apartId, creatorId);
        apartments.put(apartId, newApart);
        mongoConnector.insertApartment(apartId, newApart.getUsers(), newApart.getOwnerId());
        return apartId;
    }

    public String addSupplierToApartment(String apartmentId, String ownerId, Supplier.TYPE type) throws Exception {
        String supplierId = mongoConnector.generateIdForSupplier();
        Apartment currApart = getApartment(apartmentId);
        currApart.addSupplier(type, supplierId, ownerId);
        mongoConnector.insertSupplier(apartmentId, type.toString(), ownerId);
//        mongoConnector.insertSupplierBalances(supplierId, currSupplier.getBalances());
        return supplierId;
    }

    private Apartment getApartment(String apartmentId) throws Exception {
        Map<String, Object> apartment = mongoConnector.getApartment(apartmentId);
        return new Apartment(apartment.get("id").toString(), apartment.get("ownerId").toString());
    }


    public void addUserToApartment(String apartmentId, String userId) {
        Apartment currApartment = apartments.get(apartmentId);
        currApartment.addUser(userId);
        mongoConnector.updateApartmentUsers(apartmentId, currApartment.getUsers());
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
//        mongoConnector.updateSupplierBalances(apartmentId, currSupplier.getBalances());
    }
}