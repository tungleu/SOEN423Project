package replicaTwo.udp.request;


import replicaTwo.data.inventory.StoreInventory;
import replicaTwo.data.sales.SalesManager;
import replicaTwo.exception.ItemOutOfStockException;
import replicaTwo.exception.NotEnoughFundsException;

import java.util.Collections;
import java.util.List;

public class RequestHandlerUDP implements RequestHandler {
    private final StoreInventory storeInventory;
    private final SalesManager salesManager;

    public RequestHandlerUDP(StoreInventory storeInventory, SalesManager salesManager) {
        this.storeInventory = storeInventory;
        this.salesManager = salesManager;
    }

    @Override
    public List<String> findItem(String itemName) {
        return this.storeInventory.getStockByName(itemName);
    }

    @Override
    public List<String> purchaseItem(String itemID, int budget) {
        StringBuilder result = new StringBuilder();
        int itemPrice = this.storeInventory.getItemPrice(itemID);

        if(!this.storeInventory.isItemInStockWithQuantity(itemID)) {
            result.append(ItemOutOfStockException.class.getSimpleName());
        } else if(budget < itemPrice) {
            result.append(NotEnoughFundsException.class.getSimpleName());
        } else {
            this.storeInventory.reduceItemQuantityInStock(itemID);
            result.append(itemPrice);
        }
        return Collections.singletonList(result.toString());
    }

    @Override
    public int getItemPrice(String itemID) {
        return this.storeInventory.getItemPrice(itemID);
    }

    @Override
    public void appendToWaitQueue(String customerID, String itemID) {
        this.salesManager.appendCustomerToWaitQueue(customerID, itemID);
    }
}
