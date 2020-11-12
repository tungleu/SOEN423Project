package replicaTwo.udp.request;

import java.util.List;

public interface RequestHandler {
    List<String> purchaseItem(String itemID, int budget);
    List<String> findItem(String itemName);
    int getItemPrice(String itemID);
    void appendToWaitQueue(String customerID, String itemID);
}
