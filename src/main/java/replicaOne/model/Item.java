package replicaOne.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Kevin Tan 2020-09-20
 */
public class Item implements Serializable {

    private final String itemId;
    private String itemName;
    private double price;
    private int itemQuantity;

    public Item(String itemId, String itemName, double price, int itemQuantity) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.price = price;
        this.itemQuantity = itemQuantity;
    }

    public synchronized boolean addUserToLog(Map<String, List<PurchaseLog>> userPurchaseLog, Date date, int count) {
        if (itemQuantity > 0 && itemQuantity >= count) {
            List<PurchaseLog> purchaseCountForUser = userPurchaseLog.computeIfAbsent(itemId, k -> new ArrayList<>());
            for (int i = 0; i < count; i++) {
                purchaseCountForUser.add(new PurchaseLog(itemName, itemId, date, price));
                itemQuantity--;
            }
            return true;
        }
        return false;
    }

    public synchronized void updateQuantity(int itemQuantity) {
        this.itemQuantity += itemQuantity;
    }

    public synchronized void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public synchronized void setPrice(double price) {
        this.price = price;
    }

    public synchronized int getItemQuantity() {
        return itemQuantity;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format("{Item: %s, Item Id: %s, Quantity: %d, Price: %.2f}", itemName, itemId, itemQuantity, price);
    }
}
