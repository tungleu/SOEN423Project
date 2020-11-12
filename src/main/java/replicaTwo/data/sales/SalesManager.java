package replicaTwo.data.sales;

import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SalesManager {
    private static final int CUSTOMER_BUDGET = 1000;
    private final ConcurrentHashMap<String, Integer> customerBudget;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> customerWithExternalStores;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, TreeSet<Long>>> itemPurchaseLog;
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<String>> itemWaitList;

    public SalesManager() {
        this.customerBudget = new ConcurrentHashMap<>();
        this.customerWithExternalStores = new ConcurrentHashMap<>();
        this.itemPurchaseLog = new ConcurrentHashMap<>();
        this.itemWaitList = new ConcurrentHashMap<>();
    }

    public void checkoutRemotePurchase(String customerID, String itemID, int itemPrice, long purchaseTimestamp, String remoteStore) {
        this.checkoutLocalPurchase(customerID, itemID, itemPrice, purchaseTimestamp);
        this.customerWithExternalStores.computeIfAbsent(customerID, k -> new ConcurrentHashMap<>());
        this.customerWithExternalStores.get(customerID).computeIfAbsent(remoteStore, k -> remoteStore);
    }

    public void checkoutLocalPurchase(String customerID, String itemID, int itemPrice, long purchaseTimestamp) {
        this.itemPurchaseLog.computeIfAbsent(itemID, k -> new ConcurrentHashMap<>());
        this.itemPurchaseLog.get(itemID).computeIfAbsent(customerID, k -> new TreeSet<>());
        this.itemPurchaseLog.get(itemID).get(customerID).add(purchaseTimestamp);
        this.customerBudget.put(customerID, this.getCustomerBudget(customerID) - itemPrice);
    }

    public void refundCustomer(String targetStore, String customerID, String itemID, int itemPrice, long purchaseTimestamp) {
        this.removePurchaseTimestamp(customerID, itemID, purchaseTimestamp);
        this.increaseCustomerBudget(customerID, itemPrice);
        if(this.customerWithExternalStores.containsKey(customerID)) {
            this.customerWithExternalStores.get(customerID).remove(targetStore);
        }
    }

    public boolean isCustomerPurchasedItem(String customerID, String itemID) {
        return this.itemPurchaseLog.containsKey(itemID) && this.itemPurchaseLog.get(itemID).containsKey(customerID);
    }

    public void removePurchaseTimestamp(String customerID, String itemID, long purchaseTimestamp) {
        this.itemPurchaseLog.get(itemID).get(customerID).remove(purchaseTimestamp);
    }

    public void replacePurchaseTimestamp(String customerID, String itemID, long oldTimestamp, long newTimestamp) {
        this.itemPurchaseLog.get(itemID).get(customerID).remove(oldTimestamp);
        this.itemPurchaseLog.get(itemID).get(customerID).add(newTimestamp);
    }

    public Long getValidPurchaseTimestamp(String customerID, String itemID, long returnWindow, long returnTimestamp) {
        Long purchaseTimestamp = this.itemPurchaseLog.get(itemID).get(customerID).ceiling(returnWindow);
        if(purchaseTimestamp == null || purchaseTimestamp < returnWindow || purchaseTimestamp > returnTimestamp) {
            return null;
        }
        return purchaseTimestamp;
    }

    public boolean isCustomerWithEnoughFunds(String customerID, int itemPrice) {
        return this.getCustomerBudget(customerID) >= itemPrice;
    }

    public boolean isRemotePurchaseLimit(String customerID, String store) {
        return this.customerWithExternalStores.containsKey(customerID)
                && this.customerWithExternalStores.get(customerID).containsKey(store);
    }

    public int getCustomerBudget(String customerID) {
        this.customerBudget.putIfAbsent(customerID, CUSTOMER_BUDGET);
        return this.customerBudget.get(customerID);
    }

    public void increaseCustomerBudget(String customerID, int amount) {
        this.customerBudget.put(customerID, this.getCustomerBudget(customerID) + amount);
    }

    public void decreaseCustomerBudget(String customerID, int amount) {
        this.customerBudget.put(customerID, this.getCustomerBudget(customerID) - amount);
    }

    public void appendCustomerToWaitQueue(String customerID, String itemID) {
        this.itemWaitList.computeIfAbsent(itemID, k -> new ConcurrentLinkedDeque<>());
        this.itemWaitList.get(itemID).addLast(customerID);
    }

    public boolean isItemWithWaitList(String itemID) {
        return this.itemWaitList.containsKey(itemID) && !this.itemWaitList.get(itemID).isEmpty();
    }

    public String fetchWaitListedCustomer(String itemID) {
        return this.itemWaitList.get(itemID).pollFirst();
    }
}
