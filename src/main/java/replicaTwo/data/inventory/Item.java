package replicaTwo.data.inventory;

import java.io.Serializable;

public class Item implements Serializable {
    private final String itemID;
    private final String itemName;
    private volatile int quantity;
    private final int price;

    public Item(String itemID, String itemName, int quantity, int price) {
        this.itemID = itemID;
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
    }

    public synchronized void addQuantity(int quantity) {
        this.quantity += quantity;
    }
    public synchronized void reduceQuantity(int quantity) {
        this.quantity -= quantity;
    }
    public synchronized void setQuantityZero() {
        this.quantity = 0;
    }
    public synchronized void removeSingleQuantity() { this.quantity--; }
    public String getItemID() {
        return this.itemID;
    }
    public String getItemName() {
        return this.itemName;
    }
    public int getQuantity() {
        return this.quantity;
    }
    public int getPrice() {
        return this.price;
    }

    public String toStringByName() {
        return String.format("(itemId:%s, quantity:%d, price:%d)", this.itemID, this.quantity, this.price);
    }

    @Override
    public String toString() {
        return String.format("(itemName:%s, itemId:%s, quantity:%d, price:%d)",
                this.itemName, this.itemID, this.quantity, this.price);
    }
}