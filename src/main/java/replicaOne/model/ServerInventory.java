package replicaOne.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Kevin Tan 2020-09-21
 */
public class ServerInventory {

    private final Map<String, Double> userBudgets;
    private final Map<String, Item> inventoryCatalog;
    private final Map<String, Queue<String>> itemWaitList;
    private final Map<String, Map<String, List<PurchaseLog>>> userPurchaseLogs;
    private final Set<String> foreignCustomers;
    private final String serverName;

    public ServerInventory(String serverName) {
        this.inventoryCatalog = new ConcurrentHashMap<>();
        this.itemWaitList = new ConcurrentHashMap<>();
        this.userBudgets = new ConcurrentHashMap<>();
        this.foreignCustomers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.userPurchaseLogs = new ConcurrentHashMap<>();
        this.serverName = serverName;
    }

    public Map<String, Item> getInventoryCatalog() {
        return inventoryCatalog;
    }

    public Map<String, Queue<String>> getItemWaitList() {
        return itemWaitList;
    }

    public Set<String> getForeignCustomers() {
        return foreignCustomers;
    }

    public Map<String, Double> getUserBudgets() {
        return userBudgets;
    }

    public Map<String, Map<String, List<PurchaseLog>>> getUserPurchaseLogs() {
        return userPurchaseLogs;
    }

    public String getServerName() {
        return serverName;
    }
}
