package replicaTwo.data.inventory;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StoreInventory implements Serializable {
    private final ConcurrentHashMap<String, Item> items;

    public StoreInventory() {
        this.items = new ConcurrentHashMap<>();
    }

    public void reduceItemQuantityInStock(String itemID, int quantity) {
        this.items.computeIfPresent(itemID, (k, v) -> { v.reduceQuantity(quantity); return v; });
    }

    public void reduceItemQuantityInStock(String itemID) {
        this.items.computeIfPresent(itemID, (k, v) -> { v.removeSingleQuantity(); return v; });
    }

    public void addItemToStock(String itemID, String itemName, int quantity, int price) {
        this.items.computeIfPresent(itemID, (k, v) -> { v.addQuantity(quantity); return v; });
        this.items.computeIfAbsent(itemID, k -> new Item(itemID, itemName, quantity, price));
    }

    public void removeItemFromStock(String itemID) {
        this.items.remove(itemID);
    }

    public int getItemPrice(String itemID) {
        return this.items.containsKey(itemID) ? this.items.get(itemID).getPrice() : 0;
    }

    public String getItemName(String itemID) {
        return this.items.get(itemID).getItemName();
    }

    public boolean isEnoughItemQuantity(String itemID, int quantity) {
        if(quantity < 0) {
            return true;
        }
        return this.isItemInStock(itemID) && this.items.get(itemID).getQuantity() >= quantity;
    }

    public boolean isItemPriceMismatch(String itemID, int price) {
        return this.isItemInStock(itemID) && this.items.get(itemID).getPrice() != price;
    }

    public boolean isItemInStock(String itemID) {
        return this.items.containsKey(itemID);
    }

    public boolean isItemInStockWithQuantity(String itemID) {
        return this.items.containsKey(itemID) && this.items.get(itemID).getQuantity() > 0;
    }

    public String getItemDescription(String itemID) {
        return this.isItemInStock(itemID) ? this.items.get(itemID).toString() : "No longer in stock";
    }

    public List<String> getStockByName(String itemName) {
        return this.items.values().stream().filter(item -> item.getItemName().equals(itemName))
                .map(Item::toStringByName)
                .collect(Collectors.toList());
    }

    public String getStock() {
        return this.items.values().stream()
                .map(Item::toString)
                .collect(Collectors.joining(", "));
    }

    public Item getItem(String itemID) {
        return this.items.get(itemID);
    }
}
