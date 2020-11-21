package replicaOne.model;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Kevin Tan 2020-09-21
 */
public class ServerInventory implements Serializable {

    private final Map<String, Integer> userBudgets;
    private final Map<String, Item> inventoryCatalog;
    private final Map<String, Queue<String>> itemWaitList;
    private final Map<String, Map<String, List<PurchaseLog>>> userPurchaseLogs;
    private final Set<String> foreignCustomers;
    private final String serverName;
    private int[] ports;

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

    public Map<String, Integer> getUserBudgets() {
        return userBudgets;
    }

    public Map<String, Map<String, List<PurchaseLog>>> getUserPurchaseLogs() {
        return userPurchaseLogs;
    }

    public String getServerName() {
        return serverName;
    }

    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    public int[] getPorts() {
        return ports;
    }
}
