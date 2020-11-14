package replicaOne.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static common.OperationResponse.LIST_ITEM_AVAILABILITY_SINGLE_SUCCESS;

/**
 * Created by Kevin Tan 2020-09-20
 */
public class Item implements Serializable {

    private final String itemId;
    private String itemName;
    private int price;
    private int itemQuantity;

    public Item(String itemId, String itemName, int price, int itemQuantity) {
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

    public synchronized void setPrice(int price) {
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

    public int getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format(LIST_ITEM_AVAILABILITY_SINGLE_SUCCESS, itemName, itemId, itemQuantity, price);
    }
}
