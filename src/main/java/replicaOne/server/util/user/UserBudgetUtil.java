package replicaOne.server.util.user;

import replicaOne.model.ServerInventory;
import replicaOne.server.requests.RequestType;
import replicaOne.server.util.udp.UDPClientRequestUtil;

import java.util.function.Consumer;

import static replicaOne.server.util.IdUtil.getServerFromId;
import static replicaOne.server.util.udp.UDPClientRequestUtil.getPortForServer;

/**
 * Created by Kevin Tan 2020-09-21
 */
public final class UserBudgetUtil {

    private UserBudgetUtil() {
    }

    public static void updateUserBudget(String customerID, ServerInventory serverInventory, boolean isForeignCustomer,
                                        int price,
                                        Consumer<Integer> userLogger) {
        if (isForeignCustomer) {
            int budget = retrieveUserBudget(customerID, serverInventory, true) - price;
            String responseBudget =
                    UDPClientRequestUtil.requestFromStore(RequestType.UPDATE_BUDGET_REQ,
                            getPortForServer(getServerFromId(customerID)), customerID,
                            Integer.toString(budget));
            userLogger.accept(Integer.parseInt(responseBudget));
        } else {
            serverInventory.getUserBudgets().put(customerID, serverInventory.getUserBudgets().get(customerID) - price);
        }
    }

    public static int retrieveUserBudget(String customerID, ServerInventory serverInventory,
                                            boolean isForeignCustomer) {
        if (isForeignCustomer) {
            String budgetResponse = UDPClientRequestUtil
                    .requestFromStore(RequestType.GET_BUDGET_REQ, getPortForServer(getServerFromId(customerID)),
                            customerID);
            return Integer.parseInt(budgetResponse);
        } else {
            return serverInventory.getUserBudgets().get(customerID);
        }
    }

}