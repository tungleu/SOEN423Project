package replicaOne.server.requests;

import replicaOne.model.Item;
import replicaOne.model.Pair;
import replicaOne.model.Request;
import replicaOne.model.ServerInventory;
import replicaOne.server.util.TimeUtil;
import replicaOne.server.util.user.UserItemTransactionUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;
import java.util.List;

import static common.OperationResponse.FIND_ITEM_SINGLE_SUCCESS;
import static replicaOne.server.util.IdUtil.getServerFromId;
import static replicaOne.server.util.TimeUtil.generateTimestamp;
import static replicaOne.server.util.TimeUtil.parseStringToDate;
import static replicaOne.server.util.inventory.InventoryUtil.isEligibleForExchange;
import static replicaOne.server.util.inventory.InventoryUtil.waitListUser;

/**
 * Created by Kevin Tan 2020-09-21
 */
public class UDPServerRequestHandler {

    private static final String ERROR_MESSAGE = "Error could not complete request, please try again.";

    private final ServerInventory serverInventory;
    private final String serverName;

    public UDPServerRequestHandler(ServerInventory serverInventory) {
        this.serverInventory = serverInventory;
        this.serverName = serverInventory.getServerName();
    }

    public void handleRequestAsync(DatagramSocket aSocket, DatagramPacket request) {
        new Thread(() -> {
            try {
                String response = handleRequest(request);
                if (response == null) {
                    throw new Exception("Could not parse requests. " + ERROR_MESSAGE);
                }
                byte[] responseByte = response.getBytes();
                DatagramPacket reply =
                        new DatagramPacket(responseByte, responseByte.length, request.getAddress(), request.getPort());
                aSocket.send(reply);
                System.out.println(
                        String.format("%s Sending response back to server //%s:%d", generateTimestamp(),
                                request.getAddress().toString(),
                                request.getPort()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String handleRequest(DatagramPacket request) {
        Request parseRequest = parseIntoRequest(request);
        if (parseRequest == null) {
            return null;
        }

        List<String> params = parseRequest.getParams();

        switch (parseRequest.getRequestType()) {
            case FIND_ITEM_REQ:
                return getInventory(parseRequest.getParams().get(0));
            case PURCHASE_ITEM_REQ:
                return maybePurchaseItem(params.get(0), params.get(1), params.get(2));
            case RETURN_ITEM_REQ:
                return maybeReturnItem(params.get(0), params.get(1), params.get(2));
            case WAIT_LIST_REQ:
                return waitListUserForItem(params.get(0), params.get(1));
            case GET_BUDGET_REQ:
                return getBudget(params.get(0));
            case UPDATE_BUDGET_REQ:
                return updateBudget(params.get(0), params.get(1));
            case RETURN_ITEM_ELIGIBLE_REQ:
                return checkReturnEligibility(params.get(0), params.get(1), params.get(2));
            case EXCHANGE_ITEM_REQ:
                return exchangeItem(params);
            default:
                return ""; // should never occur

        }
    }

    private Request parseIntoRequest(DatagramPacket request) {
        ByteArrayInputStream bos = new ByteArrayInputStream(request.getData());
        Request result;
        try (ObjectInputStream out = new ObjectInputStream(bos)) {
            result = (Request) out.readObject();
            return result;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getInventory(String itemName) {
        StringBuilder sb = new StringBuilder();
        serverInventory.getInventoryCatalog().values().forEach((item) -> {
            if (item.getItemName().equals(itemName)) {
                sb.append(String.format(FIND_ITEM_SINGLE_SUCCESS, item.getItemId(), item.getItemQuantity(),
                        item.getPrice()));
                sb.append(",");
            }
        });
        return sb.toString();
    }

    private String maybePurchaseItem(String userID, String itemID, String date) {
        Item item = serverInventory.getInventoryCatalog().get(itemID);
        if (item == null) {
            String message =
                    String.format("%s Item %s does not exist in this store. Please try a different item id.",
                            TimeUtil.generateTimestamp(),
                            itemID);
            return message;
        }
        return UserItemTransactionUtil
                .maybePurchaseItem(userID, item, parseStringToDate(date), serverInventory,
                        true /* = isForeignCustomer */);
    }

    private String maybeReturnItem(String userID, String itemID, String date) {
        return UserItemTransactionUtil
                .maybeReturnItem(userID, serverInventory, true /* = isForeignCustomer */, itemID,
                        parseStringToDate(date));
    }

    private String getBudget(String userId) {
        int budget = serverInventory.getUserBudgets().get(userId);
        String message = String.format("%s Retrieved user %s budget %d", generateTimestamp(), userId, budget);
        return Integer.toString(budget);
    }

    private String updateBudget(String userId, String newBudget) {
        serverInventory.getUserBudgets().put(userId, Integer.parseInt(newBudget));
        return newBudget;
    }

    private String waitListUserForItem(String userID, String itemID) {
        return waitListUser(userID, itemID, serverInventory, true /* = isForeignCustomer*/);
    }

    private String checkReturnEligibility(String userId, String itemId, String date) {
        Date dateOfReturn = parseStringToDate(date);
        Pair<Integer, String> result =
                isEligibleForExchange(userId, serverName.equals(getServerFromId(itemId)), serverInventory, itemId,
                        dateOfReturn);
        return result.getKey() + "," + result.getValue();
    }

    private String exchangeItem(List<String> params) {
        // Deserialize into params
        String userId = params.get(0);
        int budget = Integer.parseInt(params.get(1));
        String itemIdToReturn = params.get(2);
        String itemIdToBuy = params.get(3);
        Date dateNow = parseStringToDate(params.get(4));

        Item itemToPurchase = serverInventory.getInventoryCatalog().get(itemIdToBuy);
        return UserItemTransactionUtil
                .exchangeItem(userId, budget, itemIdToReturn, itemToPurchase, dateNow, serverInventory);
    }
}
