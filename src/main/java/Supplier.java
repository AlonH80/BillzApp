import com.google.gson.internal.LinkedTreeMap;

import java.text.DecimalFormat;
import java.util.*;
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
    private MongoConnector mongoConnector;


    public Supplier(TYPE type, String apartmentId, String openerId) throws Exception {
        mongoConnector = MongoConnector.getInstance();
        logger = Utils.getLogger();
        this.supplierType = type;
        this.belongToApartmentId = apartmentId;
        ownerId = openerId;
        //balances = mongoConnector.getSupplierB;
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

//    public Map<String, Double> getBalances() {
//        Set<String> balancesKeys = balances.keySet();
//        balancesKeys.forEach(k -> {
//            Double currVal = balances.get(k);
//            DecimalFormat newFormat = new DecimalFormat("#.##");
//            balances.replace(k, Double.valueOf(newFormat.format(currVal)));
//        });
//        return balances;
//    }

    public ArrayList<Map<String, Object>> updateBalances(String newAmount, String dDay) {
        Double amount = Double.parseDouble(newAmount);
        ArrayList<Map<String, Object>> parts = mongoConnector.getSupplierParts(belongToApartmentId, supplierType.toString());
        HashMap<String, Double> partsMap = new HashMap<>(parts.size());
        parts.stream().forEach(pa -> partsMap.put(pa.get("userId").toString(), Double.parseDouble(pa.get("part").toString().replace("%", ""))/100));
        ArrayList<Map<String, Object>> currBalances = mongoConnector.getSupplierBalances(belongToApartmentId, supplierType.toString());
//        if (!currBalances.isEmpty()) {
//            currBalances.forEach(bal -> {
//                String uid = bal.get("userId").toString();
//                bal.put("balance", (Double.parseDouble(bal.get("balance").toString()) + (partsMap.get(uid) * amount)));
//                mongoConnector.updateUserSupplierBalance(bal.get("userId").toString(), belongToApartmentId, supplierType.toString(), bal.get("balance").toString());
//            });
//        }
//        else {
            partsMap.forEach((uid, part) -> {
                HashMap<String, Object> bal = new HashMap<>();
                bal.put("apartmentId", belongToApartmentId);
                bal.put("type", supplierType.toString());
                bal.put("userId", uid);
                bal.put("dDay", dDay);
                bal.put("balance", String.valueOf(part*amount));
                currBalances.add(bal);
                mongoConnector.addUserSupplierBalance(uid, belongToApartmentId, supplierType.toString(), String.valueOf(part*amount), dDay, ownerId);
            });
//        }
        mongoConnector.updateUserSupplierBalance(ownerId, belongToApartmentId, supplierType.toString(), String.valueOf(partsMap.get(ownerId)*amount), dDay);
        return currBalances;
    }

    public void updateBalanceAfterTransaction(String userIdFrom, String userIdTo, Double amount) {
        //mongoConnector.updateUserSupplierBalance(ownerId, belongToApartmentId, supplierType.toString(), String.valueOf(partsMap.get(ownerId)*amount), dDay);
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