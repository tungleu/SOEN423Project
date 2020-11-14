package replicaTwo.data.inventory;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class StoreInventoryPool implements Serializable {
    private final ConcurrentHashMap<String, StoreInventory> inventories;

    public StoreInventoryPool() {
        this.inventories = new ConcurrentHashMap<>();;
    }

    public StoreInventoryPool(StoreInventoryPool inventoryPool) {
        this.inventories = new ConcurrentHashMap<>(inventoryPool.getInventories());
    }

    public StoreInventory getInventoryOnLocation(String inventoryLocation) {
        inventories.computeIfAbsent(inventoryLocation, k -> new StoreInventory());
        return inventories.get(inventoryLocation);
    }

    public ConcurrentHashMap<String, StoreInventory> getInventories() {
        return this.inventories;
    }
}

