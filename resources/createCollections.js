db = db.getSiblingDB('billzDB');
db.suppliers.drop();
db.apartments.drop();
db.suppliersBalance.drop();
db.createCollection('suppliers');
db.createCollection('apartments');
db.createCollection('suppliersBalance');

