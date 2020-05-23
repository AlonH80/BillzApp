import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ApartsManager {

    private Map<String, Apartment> apartments;
    private static final String configPath = "/Users/alonhartanu/Desktop/Java/AppartmentManage/resources/config.json";
    private Map<String, Object> configuration;
    private Logger logger;
    private MongoConnector mongoConnector;

    public ApartsManager() throws Exception {
        configuration = Utils.jsonFileToMap(configPath);
        logger = Utils.initLogger(configuration.get("logsPath").toString(), ApartsManager.class.getName());
        apartments = new LinkedHashMap<>();
        mongoConnector = new MongoConnector(configuration.get("mongoAddress").toString(), Integer.parseInt(configuration.get("mongoPort").toString()));
        mongoConnector.setLogger(logger);
    }

    public String createApartment(String creatorId) {
        String apartId = mongoConnector.generateIdForApartment();
        Apartment newApart = new Apartment(apartId, creatorId);
        apartments.put(apartId, newApart);
        mongoConnector.insertApartment(apartId, newApart.getUsers(), newApart.getSuppliersIds(), newApart.getOwnerId());
        return apartId;
    }

    public String addSupplierToApartment(String apartmentId, String ownerId, Supplier.TYPE type) {
        String supplierId = mongoConnector.generateIdForSupplier();
        Apartment currApart = apartments.get(apartmentId);
        currApart.addSupplier(type, supplierId, ownerId);
        Supplier currSupplier = currApart.getSupplier(supplierId);
        mongoConnector.updateApartmentSuppliers(apartmentId, currApart.getSuppliersIds());
        mongoConnector.insertSupplier(supplierId, currSupplier.getBelongToApartmentId(), currSupplier.getSupplierType(), currSupplier.getOwnerId());
        mongoConnector.insertSupplierBalances(supplierId, currSupplier.getBalances());
        return supplierId;
    }

    public void addUserToApartment(String apartmentId, String userId) {
        Apartment currApartment = apartments.get(apartmentId);
        currApartment.addUser(userId);
        mongoConnector.updateApartmentUsers(apartmentId, currApartment.getUsers());
    }

    public void addUserToBill(String apartmentId, String userId, String supplierId) {
        Apartment currApartment = apartments.get(apartmentId);
        currApartment.getSupplier(supplierId).addPayer(userId);
        mongoConnector.updateApartmentUsers(apartmentId, currApartment.getUsers());
    }

    public void addNewBillToSupplier(String apartmentId, String supplierId, Double amount){
        Apartment currApart = apartments.get(apartmentId);
        Supplier currSupplier = currApart.getSupplier(supplierId);
        currSupplier.addBill(amount);
        mongoConnector.updateSupplierBalances(supplierId, currSupplier.getBalances());
    }
}
