package replicaOne.server.util.inventory;

import replicaOne.model.Item;
import replicaOne.model.Pair;
import replicaOne.model.PurchaseLog;
import replicaOne.model.ServerInventory;
import replicaOne.server.logger.ServerLogger;
import replicaOne.server.logger.UserLogger;
import replicaOne.server.util.TimeUtil;
import replicaOne.server.util.user.UserBudgetUtil;

import java.util.*;

import static common.OperationResponse.*;
import static replicaOne.server.util.IdUtil.getServerFromId;
import static replicaOne.server.util.TimeUtil.generateTimestamp;
import static replicaOne.server.util.TimeUtil.getDateNow;
import static replicaOne.server.util.user.UserItemTransactionUtil.maybePurchaseItem;

/**
 * Created by Kevin Tan 2020-10-13
 */
public final class InventoryUtil {

    private static final long MAX_RETURN_DAYS_IN_MILLIS = 2592000000L;

    private InventoryUtil() {
    }

    public static String waitListUser(String userID, String itemID, ServerInventory serverInventory, ServerLogger serverLogger,
                                      boolean isForeignCustomer) {
        Item item = serverInventory.getInventoryCatalog().get(itemID);
        String message = String.format(ADD_WAIT_LIST, userID);
        if (item != null) {
            // Sanity check: we need to ensure all other customers trying to buy the same item will be synchronous, this is because we
            // need to ensure only certain number of wait-listed users would've gotten it if the manager updated the stock concurrently
            synchronized (item) {
                if (item.getItemQuantity() > 0) {
                    /*
                     * It is possible by this time the item was updated with a new quantity.
                     * We can guarantee that this purchase will succeed as any subsequent purchases or update requests for this item will
                     * be locked until we finish processing the current request which hold the lock.
                     */
                    maybePurchaseItem(userID, item, getDateNow(), serverLogger, serverInventory, isForeignCustomer);
                    serverLogger.logAction(userID, String.format(
                            "%s Item %s was restocked while client %s was purchasing, no need to add to waitlist.", generateTimestamp(),
                            itemID, userID));

                } else {
                    Map<String, Queue<String>> itemWaitList = serverInventory.getItemWaitList();
                    Queue<String> waitListQueue = itemWaitList.computeIfAbsent(itemID, k -> new LinkedList<>());
                    waitListQueue.offer(userID);
                    serverLogger.logAction(userID, message);
                }
            }
        } else {
            // This will occur if the manager decides to remove the item in the middle of a user transaction.
            message = String.format("%s Item %s no longer exists.", TimeUtil.generateTimestamp(), itemID);
        }
        UserLogger.logActionForUser(userID, message);
        return message;
    }

    public static Pair<Integer, String> isEligibleForExchange(String userID, boolean isForeignCustomer,
                              ServerInventory serverInventory,
                              String itemId,
                              Date dateOfReturn, ServerLogger serverLogger) {
        List<PurchaseLog> purchaseLog = serverInventory.getUserPurchaseLogs().get(userID).get(itemId);
        if (purchaseLog != null) {
            int logIndex = findPossibleValidReturnDate(purchaseLog, dateOfReturn);
            if (logIndex < purchaseLog.size()) {
                int budget = UserBudgetUtil.retrieveUserBudget(userID, serverInventory, isForeignCustomer);
                return new Pair<>(budget + purchaseLog.get(logIndex).getPrice(), "Success");
            }
        }

        String message;
        if (purchaseLog == null) {
            message = String.format(EXCHANGE_ITEM_CUSTOMER_NEVER_PURCHASED, itemId);
            serverLogger.logAction(userID, String.format("%s Unable to return item since user %s did not buy any of item %s.",
                                                         TimeUtil.generateTimestamp(), userID, itemId));
        } else {
            message = String.format(EXCHANGE_ITEM_POLICY_ERROR, itemId);
            serverLogger.logAction(userID, String.format("%s Unable to return purchased item %s, over 30 day return policy.",
                                                         TimeUtil.generateTimestamp(), itemId));
        }
        return new Pair<>(-1, message);
    }

    public static void maybeLendItemsToWaitList(String userID, ServerInventory serverInventory, Item item, String serverName,
                                                ServerLogger logger) {
        String itemName = item.getItemName();
        Queue<String> users = serverInventory.getItemWaitList().get(item.getItemId());
        while (users != null && !users.isEmpty() && item.getItemQuantity() > 0) {
            String waitListedUser = users.poll();
            boolean isForeign = !getServerFromId(waitListedUser).equals(serverName);
            if (isForeign && !serverInventory.getForeignCustomers().contains(waitListedUser)) {
                logger.logAction(userID, String.format("%s Not lending item %s to user %s since they are only allowed to buy 1 item max.",
                                                       generateTimestamp(), itemName, waitListedUser));
            } else {
                logger.logAction(userID, String.format("%s Automatically trying lending item %s to user %s.", generateTimestamp(), itemName,
                                                       waitListedUser));
                // Should not fail caused by waitlisting, since when a manager updates the stock, the item will be locked
                // and will only serve the clients in the waitlist queue.
                // Only failure type can be caused by a user does not have enough money
                maybePurchaseItem(waitListedUser, item, getDateNow(), logger, serverInventory, isForeign);
            }
        }
    }

    public static int findPossibleValidReturnDate(List<PurchaseLog> purchaseLog, Date returnDate) {
        int index = 0;
        // We want to sort the purchase log to find the first date we can return
        purchaseLog.sort(Comparator.comparing(PurchaseLog::getDate));
        while (index < purchaseLog.size()) {
            PurchaseLog log = purchaseLog.get(index);
            if (returnDate.getTime() - log.getDate().getTime() <= MAX_RETURN_DAYS_IN_MILLIS) {
                return index;
            }
            index++;
        }
        return index;
    }

}
