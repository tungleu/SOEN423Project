package replicaOne.server.util.user;

import replicaOne.model.Item;
import replicaOne.model.PurchaseLog;
import replicaOne.model.ServerInventory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static common.OperationResponse.*;
import static replicaOne.server.requests.RequestType.RETURN_ITEM_REQ;
import static replicaOne.server.util.IdUtil.getServerFromId;
import static replicaOne.server.util.TimeUtil.*;
import static replicaOne.server.util.inventory.InventoryUtil.findPossibleValidReturnDate;
import static replicaOne.server.util.inventory.InventoryUtil.maybeLendItemsToWaitList;
import static replicaOne.server.util.udp.UDPClientRequestUtil.getPortForServer;
import static replicaOne.server.util.udp.UDPClientRequestUtil.requestFromStore;

/**
 * Created by Kevin Tan 2020-09-21
 */
public final class UserItemTransactionUtil {

    private UserItemTransactionUtil() {
    }

    public static String maybePurchaseItem(String userID, Item item, Date dateOfPurchase, ServerInventory serverInventory,
                                           boolean isForeignCustomer) {
        Set<String> foreignCustomers = serverInventory.getForeignCustomers();
        int budget = UserBudgetUtil.retrieveUserBudget(userID, serverInventory, isForeignCustomer);
        if (budget < 0) {
            return String.format("%s Error: Failed to retrieve user %s budget.", generateTimestamp(), userID);
        } else if (budget < item.getPrice()) {
            return String.format(PURCHASE_ITEM_NOT_ENOUGH_FUNDS, item.getItemId());
        } else if (isForeignCustomer && foreignCustomers.contains(userID)) {
            return String.format(PURCHASE_ITEM_ANOTHER_STORE_LIMIT, item.getItemId());
        }

        Map<String, List<PurchaseLog>> userPurchaseLog =
                serverInventory.getUserPurchaseLogs().computeIfAbsent(userID, k -> new ConcurrentHashMap<>());
        // Successfully passed checks
        boolean isPurchaseSuccessful = item.addUserToLog(userPurchaseLog, dateOfPurchase, 1);
        if (isPurchaseSuccessful) {
            UserBudgetUtil.updateUserBudget(userID, serverInventory, isForeignCustomer, item.getPrice());
            String message = String.format(PURCHASE_ITEM_SUCCESS, item.getItemId());
            foreignCustomers.add(userID);
            return message;
        } else {
            return String.format(PURCHASE_ITEM_OUT_OF_STOCK, item.getItemId());
        }
    }

    public static String maybeReturnItem(String userID, ServerInventory serverInventory, boolean isForeignCustomer, String itemId,
                                         Date dateOfReturn) {
        Map<String, Map<String, List<PurchaseLog>>> userPurchaseLogs = serverInventory.getUserPurchaseLogs();
        Map<String, List<PurchaseLog>> userPurchaseLog = userPurchaseLogs.computeIfAbsent(userID, k -> new ConcurrentHashMap<>());
        List<PurchaseLog> purchaseLog = userPurchaseLog.get(itemId);

        String message;
        if (purchaseLog == null) {
            message = String.format(RETURN_ITEM_CUSTOMER_NEVER_PURCHASED, itemId);
        } else {
            int index = findPossibleValidReturnDate(purchaseLog, dateOfReturn);
            if (index == purchaseLog.size()) {
                message = String.format(RETURN_ITEM_POLICY_ERROR, itemId);
            } else {
                PurchaseLog mostRecentBoughtDate = purchaseLog.remove(index);
                UserBudgetUtil.updateUserBudget(userID, serverInventory, isForeignCustomer, -mostRecentBoughtDate.getPrice());

                // Update catalog with refunded item
                updateInventoryForRefundedItem(serverInventory, itemId);

                // Remove purchase log if no more items to refund possible
                if (purchaseLog.isEmpty()) {
                    userPurchaseLog.remove(itemId);
                }
                // Allow users to purchase again
                if (isForeignCustomer) {
                    serverInventory.getForeignCustomers().remove(userID);
                }

                message = String.format(RETURN_ITEM_SUCCESS, itemId);
            }
        }
        return message;
    }

    public static String exchangeItem(int[] ports, String userId, int budget, String itemIdToReturn, String itemToBuy, Item itemToPurchase,
                                      String dateNow, ServerInventory serverInventory) {
        boolean isForeignCustomer = !getServerFromId(userId).equals(serverInventory.getServerName());
        boolean isItemToReturnLocal = getServerFromId(itemIdToReturn).equals(serverInventory.getServerName());

        // Completed ignore steps if they already purchased from store as foreign customer
        if (isForeignCustomer && serverInventory.getForeignCustomers().contains(userId) && !isItemToReturnLocal) {
            return String.format(EXCHANGE_ITEM_ANOTHER_STORE_LIMIT, itemIdToReturn, itemToPurchase.getItemId());
        }

        if (itemToPurchase == null) {
            return String.format(PURCHASE_ITEM_DOES_NOT_EXIST, itemToBuy);
        }

        String message;

        // 1. Lock the item being purchased (if available or return right away)
        synchronized (itemToPurchase) {
            // Since we already checked eligibility of return (policy + budget) we can ensure that if we return
            // successfully, we can
            // purchase the item
            if (budget < itemToPurchase.getPrice()) {
                message = String.format(EXCHANGE_ITEM_NOT_ENOUGH_FUNDS, itemIdToReturn, itemToPurchase.getItemId(), userId);
            } else if (itemToPurchase.getItemQuantity() < 1) {
                message = String.format(EXCHANGE_ITEM_OUT_OF_STOCK, itemIdToReturn, itemToPurchase.getItemId(), itemToPurchase.getItemId());
            } else {
                // 2. Perform return item, if at this point the return item fails due to manager removed item we stop
                // and don't do anything
                String returnItemResponse;
                if (isItemToReturnLocal) {
                    returnItemResponse =
                            maybeReturnItem(userId, serverInventory, isForeignCustomer, itemIdToReturn, parseStringToDate(dateNow));
                } else {
                    String server = getServerFromId(itemIdToReturn);
                    returnItemResponse =
                            requestFromStore(RETURN_ITEM_REQ, getPortForServer(ports, server), userId, itemIdToReturn, dateNow).trim();
                }

                message = returnItemResponse;

                // 3. Otherwise, if return comes back successful we proceed with purchasing the item, which in this
                // case we can guarantee
                // that we are able to purchase it as we first locked the item being purchased to prevent others from
                // doing so concurrently
                if (returnItemResponse.contains("successful")) {
                    maybePurchaseItem(userId, itemToPurchase, parseStringToDate(dateNow), serverInventory, isForeignCustomer);
                    message = String.format(EXCHANGE_ITEM_SUCCESS, itemIdToReturn, itemToPurchase.getItemId());
                }
            }
        }
        return message;
    }

    private static void updateInventoryForRefundedItem(ServerInventory serverInventory, String itemId) {
        Map<String, Item> itemCatalog = serverInventory.getInventoryCatalog();
        Item item = itemCatalog.get(itemId);
        if (item != null) {
            updateExistingItemQuantity(item, serverInventory);
        }
    }

    private static void updateExistingItemQuantity(Item item, ServerInventory serverInventory) {
        synchronized (item) {
            item.updateQuantity(1);
            maybeLendItemsToWaitList(serverInventory, item, serverInventory.getServerName());
        }
    }

}
