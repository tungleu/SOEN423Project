package replicaTwo.store;

import replica.data.ReplicaTwoData;
import replicaTwo.data.inventory.Item;
import replicaTwo.exception.*;

import java.io.IOException;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import static common.OperationResponse.*;

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

    public StoreProxy(String locationName, ReplicaTwoData replicaTwoData, Map<String, Integer> portsConfig) {
        super();
        this.logger = Logger.getLogger(locationName);
        this.store = new Store(locationName, replicaTwoData, portsConfig);
        this.locationName = locationName;
    }

    public String addItem(String managerID, String itemID, String itemName, int quantity, int price) throws IncorrectUserRoleException {
        try {
            this.validateUser(managerID, UserRole.M);
            this.validateItem(managerID, itemID);
            Item item = this.store.addItem(managerID, itemID, itemName, quantity, price);
            String operationResult = String.format(ADD_ITEM_SUCCESS, item.getItemName(), item.getItemID(), item.getQuantity(), item.getPrice());
            this.logger.info(operationResult);
            return operationResult;
        }  catch (ManagerItemPriceMismatchException e) {
            String priceMismatch = String.format(ADD_ITEM_INVALID_PRICE, price);
            this.logger.info(priceMismatch);
            return priceMismatch;
        } catch(ManagerExternalStoreItemException e) {
            String externalStore = String.format(ADD_ITEM_ANOTHER_STORE, itemName);
            this.logger.severe(externalStore);
            return externalStore;
        } catch(IncorrectUserRoleException e) {
            this.logger.severe("Permission alert! Customer with ID: " + managerID +
                    " was trying add an item: ");
            throw new IncorrectUserRoleException(e.getMessage());
        }
    }

    public String removeItem(String managerID, String itemID, int quantity) throws IncorrectUserRoleException
    {
        try {
            this.validateUser(managerID, UserRole.M);
            this.validateItem(managerID, itemID);
            String itemName = this.store.removeItem(managerID, itemID, quantity);
            String operationResult = String.format(REMOVE_ITEM_SUCCESS, itemID, itemName);
            this.logger.info(operationResult);
            return operationResult;
        } catch(ManagerRemoveNonExistingItemException e) {
            String itemNotExist = String.format(REMOVE_ITEM_NOT_EXISTS, itemID);
            this.logger.info(itemNotExist);
            return itemNotExist;
        } catch (ManagerRemoveBeyondQuantityException e) {
            String beyondQuantity = String.format(REMOVE_ITEM_BEYOND_QUANTITY, quantity, itemID);
            this.logger.info(beyondQuantity);
            return beyondQuantity;
        }catch(ManagerExternalStoreItemException e) {
            String externalStore = String.format(REMOVE_ITEM_ANOTHER_STORE, itemID);
            this.logger.severe(externalStore);
            return externalStore;
        } catch(IncorrectUserRoleException e) {
            String msg = quantity == -1 ? "completely remove" : "remove " + quantity + " units from";
            this.logger.severe("Permission alert! Customer with ID: " + managerID +
                    " was trying to " + msg + " an item with ID: " + itemID);
            throw new IncorrectUserRoleException(e.getMessage());
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
        String itemList = this.store.listItemAvailability(managerID);
        String operationResult = String.format("{%s}", itemList);
        this.logger.info(operationResult);
        return operationResult;
    }

    public String purchaseItem(String customerID, String itemID, String dateOfPurchase)throws IncorrectUserRoleException
    {
        try {
            this.validateUser(customerID, UserRole.U);
            this.store.purchaseItem(customerID, itemID, dateOfPurchase);
            String purchaseResult = String.format(PURCHASE_ITEM_SUCCESS, itemID);
            this.logger.info(purchaseResult);
            return purchaseResult;
        } catch(ItemOutOfStockException e) {
            String outOfStock = String.format(PURCHASE_ITEM_OUT_OF_STOCK, itemID);
            this.logger.info(outOfStock);
            return outOfStock;
        } catch(NotEnoughFundsException e) {
            String notEnoughFunds = String.format(PURCHASE_ITEM_NOT_ENOUGH_FUNDS, itemID);
            this.logger.info(notEnoughFunds);
            return notEnoughFunds;
        } catch(ExternalStorePurchaseLimitException e) {
            String externalStoreLimit = String.format(PURCHASE_ITEM_ANOTHER_STORE_LIMIT, itemID);
            this.logger.info(externalStoreLimit);
            return externalStoreLimit;
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
        String collectedItems = this.store.findItem(customerID, itemName);
        String operationResult = String.format("%s: {%s}", itemName, collectedItems);
        this.logger.info(operationResult);
        return operationResult;
    }

    public String returnItem(String customerID, String itemID, String dateOfReturn) throws IncorrectUserRoleException {
        try {
            this.validateUser(customerID, UserRole.U);
            this.store.returnItem(customerID, itemID, dateOfReturn);
            String operationResult = String.format(RETURN_ITEM_SUCCESS, itemID);
            this.logger.info(operationResult);
            return operationResult;
        } catch (ReturnPolicyException e) {
            String policyError = String.format(RETURN_ITEM_POLICY_ERROR, itemID);
            this.logger.info(policyError);
            return policyError;
        } catch (CustomerNeverPurchasedItemException e) {
            String neverPurchased = String.format(RETURN_ITEM_CUSTOMER_NEVER_PURCHASED, itemID);
            this.logger.info(neverPurchased);
            return neverPurchased;
        } catch (IncorrectUserRoleException e) {
            this.logger.severe("Permission alert! Manager with ID: " + customerID + " was trying to return an item" +
                    " with ID: " + itemID);
            throw new IncorrectUserRoleException(e.getMessage());
        }
    }

    public String addWaitList(String customerID, String itemID) {
        this.store.addCustomerToWaitQueue(customerID, itemID);
        return String.format(ADD_WAIT_LIST, customerID);
    }

    public String exchangeItem(String customerID, String newItemID, String oldItemID, String dateOfExchange) throws IncorrectUserRoleException {
        try {
            this.validateUser(customerID, UserRole.U);
            this.store.exchangeItem(customerID, newItemID, oldItemID, dateOfExchange);
            String exchangeStatus = String.format(EXCHANGE_ITEM_SUCCESS, oldItemID, newItemID);
            this.logger.info(exchangeStatus);
            return exchangeStatus;
        }  catch (ReturnPolicyException e) {
            String policyError = String.format(EXCHANGE_ITEM_POLICY_ERROR, oldItemID);
            this.logger.info(policyError);
            return policyError;
        } catch (CustomerNeverPurchasedItemException e) {
            String neverPurchased = String.format(EXCHANGE_ITEM_CUSTOMER_NEVER_PURCHASED, oldItemID);
            this.logger.info(neverPurchased);
            return neverPurchased;
        } catch(ExternalStorePurchaseLimitException e) {
            String externalStoreLimit = String.format(EXCHANGE_ITEM_ANOTHER_STORE_LIMIT, oldItemID, newItemID);
            this.logger.info(externalStoreLimit);
            return externalStoreLimit;
        } catch(ItemOutOfStockException e) {
            String itemOutOfStock = String.format(EXCHANGE_ITEM_OUT_OF_STOCK, oldItemID, newItemID, newItemID);
            this.logger.info(itemOutOfStock);
            return itemOutOfStock;
        } catch(NotEnoughFundsException e) {
            String outOfFunds = String.format(EXCHANGE_ITEM_NOT_ENOUGH_FUNDS, oldItemID, newItemID, customerID);
            this.logger.info(outOfFunds);
            return outOfFunds;
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
