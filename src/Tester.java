public class Tester {

    public static void main(String args[]) {
        try {
            ApartsManager apartsManager = new ApartsManager();
            String newApartId = apartsManager.createApartment("uID1");
            apartsManager.addUserToApartment(newApartId, "uID2");
            String newSuppId = apartsManager.addSupplierToApartment(newApartId, "uID1", Supplier.TYPE.ARNONA);
            apartsManager.addUserToBill(newApartId, "uID2", newSuppId);
            apartsManager.addNewBillToSupplier(newApartId, newSuppId, 25.6);
            apartsManager.addNewBillToSupplier(newApartId, newSuppId, 17.3);
            System.out.println("Done! check mongoDB");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
