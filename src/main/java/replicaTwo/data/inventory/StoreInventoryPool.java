package replicaTwo.data.inventory;

import java.util.concurrent.ConcurrentHashMap;

public class StoreInventoryPool {
    private final static ConcurrentHashMap<String, StoreInventory> inventories = new ConcurrentHashMap<>();

    public static StoreInventory getInventoryOnLocation(String inventoryLocation) {
        inventories.computeIfAbsent(inventoryLocation, k -> new StoreInventory());
        return inventories.get(inventoryLocation);
    }

    public static ConcurrentHashMap<String, StoreInventory> getInventoryPool() {
        return inventories;
    }
}

