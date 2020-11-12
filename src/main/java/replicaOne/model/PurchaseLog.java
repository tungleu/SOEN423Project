package replicaOne.model;

import java.util.Date;

/**
 * Created by Kevin Tan 2020-10-21
 */
public class PurchaseLog {

    private final String itemName;
    private final String itemId;
    private final Date date;
    private final double price;

    public PurchaseLog(String itemName, String itemId, Date date, double price) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.date = date;
        this.price = price;
    }

    public Date getDate() {
        return date;
    }

    public double getPrice() {
        return price;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemId() {
        return itemId;
    }
}
