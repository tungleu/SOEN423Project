package replicaTwo.store;

import replicaTwo.data.ReplicaData;
import replicaTwo.exception.*;

import java.io.IOException;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class StoreProxy {
    private enum UserRole {
        M('M'), U('U');
        char role;
        UserRole(char role) {
            this.role = role;
        }
        public char getRole() {
            return this.role;
        }
    }

    private final Store store;
    private final String locationName;
    private final Logger logger;

    public StoreProxy(){
        this.store = null;
        this.locationName = "";
        this.logger = null;
    }

    public StoreProxy(String locationName, ReplicaData replicaData, Map<String, Integer> portsConfig) {
        super();
        this.logger = Logger.getLogger(locationName);
        this.store = new Store(locationName, replicaData, portsConfig);
        this.locationName = locationName;
    }

    public String addItem(String managerID, String itemID, String itemName, int quantity, int price) throws IncorrectUserRoleException, ManagerExternalStoreItemException, ManagerItemPriceMismatchException {
        try {
            this.validateUser(managerID, UserRole.M);
            this.validateItem(managerID, itemID);
            String itemDescription = this.store.addItem(managerID, itemID, itemName, quantity, price);
            this.logger.info("Successfully added item. " + itemDescription);
            return itemDescription;
        }  catch (ManagerItemPriceMismatchException e) {
            this.logger.info("Manager with ID: " + managerID + " tried to add an item with ID : " + itemID + "," +
                    " but the price does not match.");
            throw new ManagerItemPriceMismatchException(e.getMessage());
        } catch(IncorrectUserRoleException e) {
            this.logger.severe("Permission alert! Customer with ID: " + managerID +
                    " was trying add an item: ");
            throw new IncorrectUserRoleException(e.getMessage());
        } catch(ManagerExternalStoreItemException e) {
            this.logger.severe("Permission alert! Manager with ID: " + managerID + " " +
                    "was trying to add an item " + itemID + " which belongs to a different store.");
            throw new ManagerExternalStoreItemException(e.getMessage());
        }
    }

    public String removeItem(String managerID, String itemID, int quantity) throws IncorrectUserRoleException, ManagerExternalStoreItemException,
            ManagerRemoveBeyondQuantityException, ManagerRemoveNonExistingItemException
    {
        try {
            this.validateUser(managerID, UserRole.M);
            this.validateItem(managerID, itemID);
            String itemDescription = this.store.removeItem(managerID, itemID, quantity);
            this.logger.info("Manager with ID: " + managerID + " removed " + quantity + "" +
                    " units from an item. " + itemDescription);
            return itemDescription;
        } catch(ManagerRemoveNonExistingItemException e) {
            String msg = quantity == -1 ? "completely remove" : "remove " + quantity + " units from";
            this.logger.info("Manager with ID: " + managerID + " was trying to " +
                    "" + msg + " an item with ID: " + itemID + "" +
                    ", but such an item does not exists in a store.");
            throw new ManagerRemoveNonExistingItemException(e.getMessage());
        } catch (ManagerRemoveBeyondQuantityException e) {
            this.logger.info("Manager with ID: " + managerID + " was trying to remove more quantity than exists in a store " +
                    "for the item with ID: " + itemID);
            throw new ManagerRemoveBeyondQuantityException(e.getMessage());
        } catch(IncorrectUserRoleException e) {
            String msg = quantity == -1 ? "completely remove" : "remove " + quantity + " units from";
            this.logger.severe("Permission alert! Customer with ID: " + managerID +
                    " was trying to " + msg + " an item with ID: " + itemID);
            throw new IncorrectUserRoleException(e.getMessage());
        } catch(ManagerExternalStoreItemException e) {
            this.logger.severe("Permission alert! Manager with ID: " + managerID + " " +
                    "was trying to remove item " + itemID + " which belongs to a different store.");
            throw new ManagerExternalStoreItemException(e.getMessage());
        }
    }

    public String listItemAvailability(String managerID) throws IncorrectUserRoleException {
        try {
            this.validateUser(managerID, UserRole.M);
        } catch(IncorrectUserRoleException e) {
            this.logger.severe("Permission alert! Customer with ID: " + managerID + "" +
                    " was trying to list available items in the store.");
            throw new IncorrectUserRoleException(e.getMessage());
        }
        this.logger.info("Manager with ID: " + managerID + " requested a list of available items.");
        return this.store.listItemAvailability(managerID);
    }

    public String purchaseItem(String customerID, String itemID, String dateOfPurchase)throws IncorrectUserRoleException,
            ItemOutOfStockException, NotEnoughFundsException, ExternalStorePurchaseLimitException {
        try {
            this.validateUser(customerID, UserRole.U);
            String purchaseResult = this.store.purchaseItem(customerID, itemID, dateOfPurchase);
            this.logger.info("Customer with ID: " + customerID + " successfully purchased an item with ID: " + itemID + "" +
                    " on " + dateOfPurchase + ".");
            return purchaseResult;
        } catch(ItemOutOfStockException e) {
            this.logger.info("Customer with ID: " + customerID + " attempted to purchase an item with ID:" +
                    " " + itemID + " on " + dateOfPurchase + ", but such an item is out of stock.");
            throw new ItemOutOfStockException(e.getMessage());
        } catch(NotEnoughFundsException e) {
            this.logger.info("Customer with ID: " + customerID + " attempted to purchase an item with" +
                    " ID: " + itemID + " on " + dateOfPurchase + ", but does not have enough funds.");
            throw new NotEnoughFundsException(e.getMessage());
        } catch(ExternalStorePurchaseLimitException e) {
            this.logger.info("Customer with ID: " + customerID + " attempted to purchase an item with" +
                    " ID: " + itemID + " on " + dateOfPurchase + ", but he/she already made purchase from " +
                    "" + itemID.substring(0, 2) + " store.");
            throw new ExternalStorePurchaseLimitException(e.getMessage());
        } catch (IncorrectUserRoleException e) {
            this.logger.severe("Permission alert! Manager with ID: " + customerID + "" +
                    " was trying to purchase an item with ID: " + itemID + " on " + dateOfPurchase);
            throw new IncorrectUserRoleException(e.getMessage());
        }
    }

    public String findItem(String customerID, String itemName) throws IncorrectUserRoleException {
        try {
            this.validateUser(customerID, UserRole.U);
        } catch (IncorrectUserRoleException e) {
            this.logger.severe("Permission alert! Manager with ID: " + customerID + " " +
                    "was trying to find items with " + itemName + " name.");
            throw new IncorrectUserRoleException(e.getMessage());
        }
        String result = this.store.findItem(customerID, itemName);
        this.logger.info("Customer with ID: " + customerID + " requested to find all items based on " + itemName + " name.");
        return result;
    }

    public String returnItem(String customerID, String itemID, String dateOfReturn) throws ReturnPolicyException,
            CustomerNeverPurchasedItemException, IncorrectUserRoleException {
        try {
            this.validateUser(customerID, UserRole.U);
            String returnStatus = this.store.returnItem(customerID, itemID, dateOfReturn);
            this.logger.info("Successfully returned item with ID: " + itemID + " purchased by the customer " +
                    "with ID: " + customerID + " on " + dateOfReturn);
            return returnStatus;
        } catch (ReturnPolicyException e) {
            this.logger.info("Customer with ID: " + customerID + " tried to return an item with ID: " + itemID + "" +
                    " , but it is beyond the return policy.");
            throw new ReturnPolicyException(e.getMessage());
        } catch (CustomerNeverPurchasedItemException e) {
            this.logger.info("Customer with ID: " + customerID + " tried to return an item with ID: " + itemID + "" +
                    " , but the customer never purchased such an item.");
            throw new CustomerNeverPurchasedItemException(e.getMessage());
        } catch (IncorrectUserRoleException e) {
            this.logger.severe("Permission alert! Manager with ID: " + customerID + " was trying to return an item" +
                    " with ID: " + itemID);
            throw new IncorrectUserRoleException(e.getMessage());
        }

    }

    public String addCustomerToWaitQueue(String customerID, String itemID) {
        return this.store.addCustomerToWaitQueue(customerID, itemID);
    }

    public String exchangeItem(String customerID, String newItemID, String oldItemID, String dateOfExchange) throws ReturnPolicyException,
            CustomerNeverPurchasedItemException, ExternalStorePurchaseLimitException, ItemOutOfStockException, NotEnoughFundsException, IncorrectUserRoleException {
        try {
            this.validateUser(customerID, UserRole.U);
            String exchangeStatus = this.store.exchangeItem(customerID, newItemID, oldItemID, dateOfExchange);
            this.logger.info("Customer with ID: " + customerID + " has successfully exchanged an item with ID: " + oldItemID + "" +
                    " for an item with ID: " + newItemID + " on " + dateOfExchange);
            return exchangeStatus;
        }  catch (ReturnPolicyException e) {
            this.logger.info("Customer with ID: " + customerID + " tried to exchange an item with ID: " + oldItemID + "" +
                    " , for a new item with ID: " + newItemID + ", but it is beyond the return policy.");
            throw new ReturnPolicyException(e.getMessage());
        } catch (CustomerNeverPurchasedItemException e) {
            this.logger.info("Customer with ID: " + customerID + " tried to return an item with ID: " + oldItemID + "" +
                    " , but the customer never purchased such an item.");
            throw new CustomerNeverPurchasedItemException(e.getMessage());
        } catch(ExternalStorePurchaseLimitException e) {
            this.logger.info("Customer with ID: " + customerID + " attempted to exchange an item with" +
                    " ID: " + oldItemID + " on " + dateOfExchange + " for an item with ID: " + newItemID + ", but he/she already made purchase from " +
                    "" + newItemID.substring(0, 2) + " store.");
            throw new ExternalStorePurchaseLimitException(e.getMessage());
        } catch(ItemOutOfStockException e) {
            this.logger.info("Customer with ID: " + customerID + " attempted to exchange an item with ID:" +
                    " " + oldItemID + " on " + dateOfExchange + " for an item with ID: " + newItemID + ", but such an item is out of stock.");
            throw new ItemOutOfStockException(e.getMessage());
        } catch(NotEnoughFundsException e) {
            this.logger.info("Customer with ID: " + customerID + " attempted to exchange an item with" +
                    " ID: " + oldItemID + " on " + dateOfExchange + " for an item with ID: " + newItemID + ", but does not have enough funds.");
            throw new NotEnoughFundsException(e.getMessage());
        } catch (IncorrectUserRoleException e) {
            this.logger.severe("Permission alert! Manager with ID: " + customerID + " was trying to exchange an item" +
                    " with ID: " + oldItemID + " to a new item with ID: " + newItemID + " on " + dateOfExchange);
            throw new IncorrectUserRoleException(e.getMessage());
        }

    }

    public void initializeStore(int port) throws IOException {
        this.store.listen(port);
        setupLogger();
    }

    private void validateUser(String userID, UserRole expectedRole) throws IncorrectUserRoleException {
        char currentRole = userID.charAt(2);
        if(currentRole != expectedRole.getRole()) {
            throw new IncorrectUserRoleException("Invalid User ID");
        }
    }

    private void validateItem(String managerID, String itemID) throws ManagerExternalStoreItemException {
        String managerStore = managerID.substring(0, 2);
        String itemStore = itemID.substring(0, 2);
        if(!managerStore.equals(itemStore)) {
            throw new ManagerExternalStoreItemException("Manager is trying to add item from a different store!");
        }
    }

    private void setupLogger() throws IOException {
        String logFile = this.locationName + ".log";
        Handler fileHandler  = new FileHandler(System.getProperty("user.dir") + "/src/main/java/replicaTwo/logs/" + logFile, true);
        this.logger.setUseParentHandlers(false);
        this.logger.addHandler(fileHandler);
    }
}
