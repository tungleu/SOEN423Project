package replicaTwo.store;

import common.StoreStrategy;
import replica.data.ReplicaTwoData;
import replicaTwo.exception.*;
import java.util.Map;

import static common.OperationResponse.*;

public class StoreProxy implements StoreStrategy {
    private final Store store;

    public StoreProxy(String locationName, ReplicaTwoData replicaTwoData, Map<String, Integer> portsConfig) {
        super();
        this.store = new Store(locationName, replicaTwoData, portsConfig);
    }

    @Override
    public String addItem(String managerID, String itemID, String itemName, int quantity, int price) {
        try {
            this.validateItem(managerID, itemID);
            this.store.addItem(managerID, itemID, itemName, quantity, price);
            return String.format(ADD_ITEM_SUCCESS, itemName, itemID, quantity, price);
        }  catch (ManagerItemPriceMismatchException e) {
            return String.format(ADD_ITEM_INVALID_PRICE, price);
        } catch(ManagerExternalStoreItemException e) {
            return String.format(ADD_ITEM_ANOTHER_STORE, itemName);
        }
    }

    @Override
    public String removeItem(String managerID, String itemID, int quantity)
    {
        try {
            this.validateItem(managerID, itemID);
            String itemName = this.store.removeItem(managerID, itemID, quantity);
            return String.format(REMOVE_ITEM_SUCCESS, itemName, itemID);
        } catch(ManagerRemoveNonExistingItemException e) {
            return String.format(REMOVE_ITEM_NOT_EXISTS, itemID);
        } catch (ManagerRemoveBeyondQuantityException e) {
            return String.format(REMOVE_ITEM_BEYOND_QUANTITY, quantity, itemID);
        }catch(ManagerExternalStoreItemException e) {
            return String.format(REMOVE_ITEM_ANOTHER_STORE, itemID);
        }
    }

    @Override
    public String listItemAvailability(String managerID) {
        String itemList = this.store.listItemAvailability(managerID);
        return String.format("{%s}", itemList);
    }

    @Override
    public String purchaseItem(String customerID, String itemID, String dateOfPurchase)
    {
        try {
            this.store.purchaseItem(customerID, itemID, dateOfPurchase);
            return String.format(PURCHASE_ITEM_SUCCESS, itemID);
        } catch(ItemDoesNotExistException e) {
            return String.format(PURCHASE_ITEM_DOES_NOT_EXIST, itemID);
        } catch(ItemOutOfStockException e) {
            return String.format(PURCHASE_ITEM_OUT_OF_STOCK, itemID);
        } catch(NotEnoughFundsException e) {
            return String.format(PURCHASE_ITEM_NOT_ENOUGH_FUNDS, itemID);
        } catch(ExternalStorePurchaseLimitException e) {
            return String.format(PURCHASE_ITEM_ANOTHER_STORE_LIMIT, itemID);
        }
    }

    @Override
    public String findItem(String customerID, String itemName) {
        String collectedItems = this.store.findItem(customerID, itemName);
        return String.format("%s: {%s}", itemName, collectedItems);
    }

    @Override
    public String returnItem(String customerID, String itemID, String dateOfReturn) {
        try {
            this.store.returnItem(customerID, itemID, dateOfReturn);
            return String.format(RETURN_ITEM_SUCCESS, itemID);
        } catch (ReturnPolicyException e) {
            return String.format(RETURN_ITEM_POLICY_ERROR, itemID);
        } catch (CustomerNeverPurchasedItemException e) {
            return String.format(RETURN_ITEM_CUSTOMER_NEVER_PURCHASED, itemID);
        }
    }

    @Override
    public String addWaitList(String customerID, String itemID) {
        this.store.addCustomerToWaitQueue(customerID, itemID);
        return String.format(ADD_WAIT_LIST, customerID);
    }

    @Override
    public String exchangeItem(String customerID, String newItemID, String oldItemID, String dateOfExchange) {
        try {
            this.store.exchangeItem(customerID, newItemID, oldItemID, dateOfExchange);
            return String.format(EXCHANGE_ITEM_SUCCESS, oldItemID, newItemID);
        } catch(ItemDoesNotExistException e) {
            return String.format(PURCHASE_ITEM_DOES_NOT_EXIST, newItemID);
        } catch (ReturnPolicyException e) {
            return String.format(EXCHANGE_ITEM_POLICY_ERROR, oldItemID);
        } catch (CustomerNeverPurchasedItemException e) {
            return String.format(EXCHANGE_ITEM_CUSTOMER_NEVER_PURCHASED, oldItemID);
        } catch(ExternalStorePurchaseLimitException e) {
            return String.format(EXCHANGE_ITEM_ANOTHER_STORE_LIMIT, oldItemID, newItemID);
        } catch(ItemOutOfStockException e) {
            return String.format(EXCHANGE_ITEM_OUT_OF_STOCK, oldItemID, newItemID, newItemID);
        } catch(NotEnoughFundsException e) {
            return String.format(EXCHANGE_ITEM_NOT_ENOUGH_FUNDS, oldItemID, newItemID, customerID);
        }
    }

    public void initializeStore(int port) {
        this.store.listen(port);
    }

    private void validateItem(String managerID, String itemID) throws ManagerExternalStoreItemException {
        String managerStore = managerID.substring(0, 2);
        String itemStore = itemID.substring(0, 2);
        if(!managerStore.equals(itemStore)) {
            throw new ManagerExternalStoreItemException("Manager is trying to add item from a different store!");
        }
    }
}
