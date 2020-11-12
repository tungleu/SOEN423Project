package replicaTwo.data;

import replicaTwo.data.inventory.StoreInventoryPool;
import replicaTwo.data.inventory.StoreInventory;
import replicaTwo.data.sales.SalesManager;
import replicaTwo.data.sales.SalesManagerPool;

import java.util.concurrent.ConcurrentHashMap;

public class ReplicaData {
    private ConcurrentHashMap<String, StoreInventory> inventories;
    private ConcurrentHashMap<String, SalesManager> salesManagers;

    public ReplicaData() {
        this.inventories = new ConcurrentHashMap<>(StoreInventoryPool.getInventoryPool());
        this.salesManagers = new ConcurrentHashMap<>(SalesManagerPool.getManagersPool());
    }

    public SalesManager getSalesManagerOnLocation(String location) {
        return this.salesManagers.get(location);
    }

    public StoreInventory getInventoryOnLocation(String location) {
        return this.inventories.get(location);
    }
}
