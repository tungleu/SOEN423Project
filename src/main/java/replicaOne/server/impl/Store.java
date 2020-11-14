package replicaOne.server.impl;

import common.StoreStrategy;
import javafx.util.Pair;
import replicaOne.model.Item;
import replicaOne.model.ServerInventory;
import replicaOne.server.logger.ServerLogger;
import replicaOne.server.logger.UserLogger;
import replicaOne.server.util.IdUtil;
import replicaOne.server.util.user.UserItemTransactionUtil;

import java.util.*;

import static common.OperationResponse.*;
import static replicaOne.server.requests.RequestType.*;
import static replicaOne.server.util.IdUtil.getServerFromId;
import static replicaOne.server.util.TimeUtil.*;
import static replicaOne.server.util.inventory.InventoryUtil.*;
import static replicaOne.server.util.udp.UDPClientRequestUtil.*;
import static replicaOne.server.util.user.UserItemTransactionUtil.maybePurchaseItem;
import static replicaOne.server.util.user.UserItemTransactionUtil.maybeReturnItem;

public class Store implements StoreStrategy {

    // Server information & logger
    private final String serverName;
    private final int serverPort;
    private final ServerLogger logger;

    // Server Data
    private final Map<String, Item> inventoryCatalog;
    private final Map<String, Queue<String>> itemWaitList;
    private final ServerInventory serverInventory;

    public Store(String serverName, int port, ServerInventory serverInventory, ServerLogger logger) {
        this.inventoryCatalog = serverInventory.getInventoryCatalog();
        this.itemWaitList = serverInventory.getItemWaitList();
        this.serverInventory = serverInventory;
        this.logger = logger;
        this.serverName = serverName;
        this.serverPort = port;
    }

    /* Manager Requests */

    @Override
    public String addItem(String userID, String itemID, String itemName, int itemQuantity, int itemPrice) {
        logger.logAction(userID, String.format("%s Request: %s, Params: [User: %s, Item id: %s, Item name: %s, " +
                        "Quantity: %d, Price: %d]",
                generateTimestamp(), "addItem", userID, itemID, itemName, itemQuantity, itemPrice));
        try {
            IdUtil.checkHasAccess(serverInventory, userID, true /* = isManager */);
        } catch (Exception e) {
            return e.getMessage();
        }
        String message;
        Item item = inventoryCatalog.get(itemID);
        if (!getServerFromId(itemID).equals(serverName)) {
            message = String.format(ADD_ITEM_ANOTHER_STORE, itemName);
        } else if (itemQuantity <= 0) {
            message = String.format(ADD_ITEM_INVALID_QUANT, itemQuantity);
        } else if (item == null) {
            item = new Item(itemID, itemName, itemPrice, itemQuantity);
            inventoryCatalog.put(itemID, item);
            message = String.format(ADD_ITEM_SUCCESS, itemName, itemID, itemQuantity, itemPrice);
        } else if (item.getPrice() != itemPrice) {
            message = String.format(ADD_ITEM_INVALID_PRICE, itemPrice);
        } else {
            // We need to prevent customers from purchasing the item while it is being updated
            synchronized (item) {
                // First we update the name & price before performing automatic purchases for clients
                item.setPrice(itemPrice);
                item.setItemName(itemName);
                updateItemQuantity(userID, item, itemQuantity);
                maybeLendItemsToWaitList(userID, serverInventory, item, serverName, logger);
                message = String.format(ADD_ITEM_SUCCESS, itemName, itemID, itemQuantity, itemPrice);
            }
        }
        UserLogger.logActionForUser(userID, message);
        logger.logAction(userID, message);
        return message;
    }

    @Override
    public String removeItem(String userID, String itemID, int quantity) {
        logger.logAction(userID,
                String.format("%s Request: %s, Params: [User: %s, Item id: %s, Quantity: %d]", generateTimestamp(),
                        "removeItem",
                        userID, itemID, quantity));
        try {
            IdUtil.checkHasAccess(serverInventory, userID, true /* = isManager */);
        } catch (Exception e) {
            return e.getMessage();
        }
        Item item = inventoryCatalog.get(itemID);
        String message;
        if (!getServerFromId(itemID).equals(serverName)) {
            message = String.format(REMOVE_ITEM_ANOTHER_STORE, itemID);
        } else if (item != null) {
            if (quantity > item.getItemQuantity()) {
                message = String.format(REMOVE_ITEM_BEYOND_QUANTITY, quantity, itemID);
            } else {
                // Removing negative numbers automatically removes the item
                synchronized (item) {
                    int itemsToRemove = (quantity <= 0) ? 0 : -quantity;
                    updateItemQuantity(userID, item, itemsToRemove);
                }
                message = String.format(REMOVE_ITEM_SUCCESS, item.getItemName(), itemID);
            }
        } else {
            message = String.format(REMOVE_ITEM_NOT_EXISTS, itemID);
        }

        UserLogger.logActionForUser(userID, message);
        logger.logAction(userID, message);

        return message;
    }

    @Override
    public String listItemAvailability(String userID) {
        logger.logAction(userID,
                String.format("%s Request: %s, Params: [User: %s]", generateTimestamp(), "listItemAvailability",
                        userID));
        try {
            IdUtil.checkHasAccess(serverInventory, userID, true /* = isManager */);
        } catch (Exception e) {
            return null;
        }
        Iterator<Item> results = inventoryCatalog.values().iterator();

        StringBuilder response = new StringBuilder();
        response.append("{");
        while (results.hasNext()) {
            response.append(results.next());
            if (results.hasNext()) response.append(",");
        }
        response.append("}");
        return response.toString();
    }

    /* Customer Requests */

    @Override
    public String purchaseItem(String userID, String itemId, String dateOfPurchase) {
        logger.logAction(userID,
                String.format("%s Request: %s, Params: [User: %s, Item id: %s, PurchaseDate: %s]", generateTimestamp(),
                        "purchaseItem", userID, itemId, dateOfPurchase));
        try {
            IdUtil.checkHasAccess(serverInventory, userID, false /* = isManager */);
        } catch (Exception e) {
            return e.getMessage();
        }
        Date parsedDate = parseStringToDate(dateOfPurchase);
        String server = getServerFromId(itemId);
        String response;
        if (server.equals(serverName)) {
            // Perform local server check
            Item item = inventoryCatalog.get(itemId);
            if (item == null) {
                String message =
                        String.format("%s Purchase un-successfully, item with item id %s does not exists in the store.",
                                generateTimestamp(), itemId);
                logger.logAction(userID, message);
                UserLogger.logActionForUser(userID, message);
                return message;
            }
            response = maybePurchaseItem(userID, item, parsedDate, logger, serverInventory,
                    false /* = isForeignCustomer */);
        } else {
            // UDP to other server
            logger.logAction(userID,
                    String.format("%s UDP Request to server %s with Params: [User: %s, Item id: %s, PurchaseDate: %s]",
                            generateTimestamp(), server, userID, itemId, dateOfPurchase));
            response =
                    requestFromStore(PURCHASE_ITEM_REQ, getPortForServer(server), userID, itemId,
                            dateOfPurchase).trim();
        }
        return response;
    }

    @Override
    public String findItem(String userID, String itemName) {
        logger.logAction(userID,
                String.format("%s Request: %s, Params: [User: %s, Item name: %s]", generateTimestamp(), "findItem",
                        userID,
                        itemName));
        try {
            IdUtil.checkHasAccess(serverInventory, userID, false /* = isManager */);
        } catch (Exception e) {
            return null;
        }

        List<String> results = new LinkedList<>();

        // Current store
        inventoryCatalog.values().forEach(
                (item) -> results.add(String.format(FIND_ITEM_SINGLE_SUCCESS, item.getItemId(), item.getItemQuantity(),
                        item.getPrice())));
        // UDP req to other stores for items
        for (int port : PORTS) {
            if (serverPort != port) {
                String responseString = requestFromStore(FIND_ITEM_REQ, port, itemName);
                logger.logAction(userID,
                        String.format("%s Result from UDP to server port %d findItem: %s", generateTimestamp(), port,
                                responseString));
                String response = responseString.trim();

                // Ensure we ignore "," responses
                if (response.length() > 1) {
                    results.add(response);
                }
            }
        }

        StringBuilder sb = new StringBuilder(itemName).append(": {");
        for (String result : results) {
            sb.append(result);
            if (result.charAt(result.length() - 1) != ',') sb.append(",");
        }

        // Cleanup
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
        sb.append("}");

        return sb.toString();
    }

    @Override
    public String returnItem(String userID, String itemId, String dateOfReturn) {
        logger.logAction(userID,
                String.format("%s Request: %s, Params: [User: %s, Item id: %s, ReturnDate: %s]", generateTimestamp(),
                        "returnItem",
                        userID, itemId, dateOfReturn));
        try {
            IdUtil.checkHasAccess(serverInventory, userID, false /* = isManager */);
        } catch (Exception e) {
            return e.getMessage();
        }
        String server = getServerFromId(itemId);
        if (server.equals(serverName)) {
            return maybeReturnItem(userID, serverInventory, false /* = isForeignCustomer */, itemId,
                    parseStringToDate(dateOfReturn),
                    logger);
        } else {
            // UDP to other server
            logger.logAction(userID,
                    String.format("%s UDP Request to server %s with Params: [User: %s, Item id: %s, ReturnDate: %s]",
                            generateTimestamp(), server, userID, itemId, dateOfReturn));
            return requestFromStore(RETURN_ITEM_REQ, getPortForServer(server), userID, itemId,
                    dateOfReturn).trim();
        }
    }

    /**
     * The exchangeItem method will first attempt to retrieve the potential budget value of a customer after they
     * have performed a refund.
     * This facilitates the subsequent requests to perform the exchange. To understand the flow and synchronization
     * of this method, the
     * steps
     * are listed as follows:
     * 1. Retrieve the potential budget for the customer after a successful return. This step will also check if the
     * customer is able to
     * return
     * the item, if customer is not the retrieved budget will be -1.
     * 2. If customer is eligible to return the item (budget retrieved > 0), then the exchange can begin.
     * 3. The system will find the server where the purchase item lives on and send a UDP request to begin the
     * exchange on that server.
     * 4. Lock the item being purchased.
     * 5. Check if user has enough budget and item quantity is > 0 (these are all thread-safe operations now)
     * 6. If successful, perform refund (UDP or local)
     * 7. If refund was successful, proceed with purchasing item (this is a guaranteed success)
     * <p>
     * Note: Since the item to return and item to purchase can be from any store, it is important that it the process
     * takes place on the
     * server where the item to purchase resides so the item can be locked until the exchange is complete to prevent
     * any concurrent
     * requests that attempts to purchase/update the item. This is because this method requires to be atomic, since
     * it was already
     * validated that the budget is correct from the first step, then if the return is successful it is guaranteed
     * that the purchase can
     * happen, otherwise we skip the purchase and stop. If the item was not locked, then it could be possible that
     * the item was purchased
     * by another user while it was attempting to return an item (since the return could be a UDP request to another
     * server).
     */
    @Override
    public String exchangeItem(String customerID, String newItemID, String oldItemID, String dateOfReturn) {
        logger.logAction(customerID,
                String.format("%s Request: %s, Params: [User: %s, New Item id: %s, Old Item id: %s]",
                        generateTimestamp(),
                        "exchangeItem", customerID, newItemID, oldItemID));
        try {
            IdUtil.checkHasAccess(serverInventory, customerID, false /* = isManager */);
        } catch (Exception e) {
            return e.getMessage();
        }

        Date parsedReturnDate = parseStringToDate(dateOfReturn);
        Pair<Integer, String> exchangeResp = checkEligibleForExchange(customerID, oldItemID, parsedReturnDate);

        int budget = exchangeResp.getKey();
        if (budget > 0) {
            return exchangeItem(customerID, oldItemID, newItemID, budget, parsedReturnDate);
        }

        String message = exchangeResp.getValue();
        logger.logAction(customerID, message);
        UserLogger.logActionForUser(customerID, message);

        return message;
    }

    @Override
    public String addWaitList(String userID, String itemID) {
        String server = getServerFromId(itemID);
        if (serverName.equals(server)) {
            String response = waitListUser(userID, itemID, serverInventory, logger, false /* = isForeignCustomer */);
            logger.logAction(userID, response);
            return response;
        } else {
            return requestFromStore(WAIT_LIST_REQ, getPortForServer(server), userID, itemID);
        }
    }

    private void updateItemQuantity(String userID, Item item, int newQuantity) {
        String itemId = item.getItemId();
        int oldQuantity = item.getItemQuantity();
        String message;
        if (newQuantity <= 0) {
            item.updateQuantity(newQuantity);
            int updatedQuantity = item.getItemQuantity();
            if (newQuantity == 0 || updatedQuantity < 0) {
                // Lock inventory to prevent subsequent purchase requests (any subsequent request will no longer find
                // it)
                // Will also allow garbage collection to free up memory since we no longer will hold ref after this
                // method exits
                inventoryCatalog.remove(itemId);
                itemWaitList.remove(itemId);
                // We keep this to ensure that any requests that came first before this cancel
                // They will attempt to be put on the wait list but the above line will ensure it could not
                message = String.format("%s Item id: %s was successfully deleted", generateTimestamp(), itemId);
            } else {
                message = String.format("%s Item id: %s was successfully updated quantity to %d", generateTimestamp(),
                        itemId,
                        updatedQuantity);
            }
            UserLogger.logActionForUser(userID, message);
            logger.logAction(userID, message);
        } else {
            item.updateQuantity(newQuantity);
            message = String.format("%s Item id: %s quantity updated to %d from %d", generateTimestamp(), itemId,
                    newQuantity, oldQuantity);
            logger.logAction(userID, message);
            UserLogger.logActionForUser(userID, message);
        }
    }

    private Pair<Integer, String> checkEligibleForExchange(String userId, String itemId, Date dateOfReturn) {
        String server = getServerFromId(itemId);
        if (server.equals(serverName)) {
            return isEligibleForExchange(userId, false /* = isForeignCustomer */, serverInventory, itemId, dateOfReturn,
                    logger);
        } else {
            String response =
                    requestFromStore(RETURN_ITEM_ELIGIBLE_REQ, getPortForServer(server), userId, itemId,
                            parseDateToString(dateOfReturn))
                            .trim();
            String[] values = response.split(",");
            return new Pair<>(Integer.parseInt(values[0]), values[1]);
        }
    }

    private String exchangeItem(String userId, String oldItemId, String newItemId, int budget, Date dateNow) {
        String newItemServer = getServerFromId(newItemId);
        if (newItemServer.equals(serverName)) {
            Item item = inventoryCatalog.get(newItemId);
            return UserItemTransactionUtil
                    .exchangeItem(userId, budget, oldItemId, item, dateNow, serverInventory, logger);
        } else {
            return requestFromStore(EXCHANGE_ITEM_REQ, getPortForServer(newItemServer), userId, Double.toString(budget),
                    oldItemId,
                    newItemId, parseDateToString(dateNow)).trim();
        }
    }

}
