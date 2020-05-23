import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Apartment {

    private String apartmentId;
    private List<String> userIds;
    private String ownerId;
    private List<Supplier> suppliers;
    private Logger logger;

    private static String idGenerator() {
        return "";
    }

    public Apartment(String id, String ownerId) {
        this.apartmentId = id;
        this.ownerId = ownerId;
        userIds = new ArrayList<>();
        userIds.add(ownerId);
        suppliers = new ArrayList<>();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void addSupplier(Supplier.TYPE type, String supplierId, String openerId) {
        Supplier newSupplier = new Supplier(type, supplierId, apartmentId, openerId);
        suppliers.add(newSupplier);
    }

    public void removeSupplier(String supplierId) {
        Object[] matchedSuppliers = suppliers.stream().filter(supp->supp.getSupplierId().equals(supplierId)).toArray();
        if (matchedSuppliers.length > 0) {
            suppliers.remove(matchedSuppliers[0]);
        }
        else {
            logger.warning(String.format("Supplier %s wasn't found", supplierId));
        }
    }

    public void addUser(String userId) {
        userIds.add(userId);
    }

    public void removeUser(String userId) {
        if (userIds.contains(userId)) {
            userIds.remove(userId);
        }
        else {
            logger.warning(String.format("User %s doesn't exist in this appartment(%s)", userId, apartmentId));
        }
    }

    public void balance() {

    }

    public List<String> getUsers(){
        return userIds;
    }

    public List<String> getSuppliersIds() {
        return suppliers.stream().map(Supplier::getSupplierId).collect(Collectors.toList());
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Supplier getSupplier(String supplierId) {
        return suppliers.stream().filter(sup->sup.getSupplierId().equals(supplierId)).collect(Collectors.toList()).get(0);
    }

    public String getApartmentId() {
        return apartmentId;
    }
}
