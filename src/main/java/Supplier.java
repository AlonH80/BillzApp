import com.google.gson.internal.LinkedTreeMap;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class Supplier {

    public enum TYPE {
        ELECTRICITY,
        WATER,
        PROPERTY_TAX,
        RENT,
        OTHER
    }

    private String supplierId;
    private String belongToApartmentId;
    private TYPE supplierType;
    private String ownerId;
    private Map<String, Double> balances;
    private Logger logger;


    public Supplier(TYPE type, String apartmentId, String openerId) throws Exception {
        logger = Utils.getLogger();
        this.supplierType = type;
        this.belongToApartmentId = apartmentId;
        ownerId = openerId;
        balances = new LinkedHashMap<>();
    }

    public void addPayer(String payerId) {
        Double newValue = 0.0;
        balances.put(payerId, 0.0);
    }

    public void removePayer(String payerId) {
        if (balances.containsKey(payerId)) {
            if (balances.get(payerId) > 0) {
                logger.warning(String.format("%s still has a debt to owner. Amount: %.2f", payerId, balances.get(payerId)));
            }
            balances.remove(payerId);
        } else {
            logger.warning(String.format("key '%s' doesn't exist in this supplier balances", payerId));
        }
    }

    public void addBill(Double amount) {
        Double partPerUser = amount / (balances.size() + 1);
        Set<String> balancesKeys = balances.keySet();
        balancesKeys.forEach(k -> balances.put(k, balances.get(k) + partPerUser));
    }

    public String getSupplierId() {
        return supplierId;
    }

    public Map<String, Double> getBalances() {
        Set<String> balancesKeys = balances.keySet();
        balancesKeys.forEach(k -> {
            Double currVal = balances.get(k);
            DecimalFormat newFormat = new DecimalFormat("#.##");
            balances.replace(k, Double.valueOf(newFormat.format(currVal)));
        });
        return balances;
    }

    public String getBelongToApartmentId() {
        return belongToApartmentId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public TYPE getSupplierType() {
        return supplierType;
    }
}