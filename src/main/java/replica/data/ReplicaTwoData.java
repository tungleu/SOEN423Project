package replica.data;

import replicaTwo.data.inventory.StoreInventoryPool;
import replicaTwo.data.inventory.StoreInventory;
import replicaTwo.data.sales.SalesManager;
import replicaTwo.data.sales.SalesManagerPool;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicaTwoData implements Serializable {
    private final ConcurrentHashMap<String, StoreInventory> inventories;
    private final ConcurrentHashMap<String, SalesManager> salesManagers;

    public ReplicaTwoData() {
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
