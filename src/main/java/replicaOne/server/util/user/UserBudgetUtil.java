package replicaOne.server.util.user;

import replicaOne.model.ServerInventory;
import replicaOne.server.requests.RequestType;
import replicaOne.server.util.udp.UDPClientRequestUtil;

import static replicaOne.server.util.IdUtil.getServerFromId;
import static replicaOne.server.util.udp.UDPClientRequestUtil.getPortForServer;

/**
 * Created by Kevin Tan 2020-09-21
 */
public final class UserBudgetUtil {

    private UserBudgetUtil() {
    }

    public static void updateUserBudget(String customerID, ServerInventory serverInventory, boolean isForeignCustomer, int price) {
        if (isForeignCustomer) {
            int budget = retrieveUserBudget(customerID, serverInventory, true) - price;
            UDPClientRequestUtil.requestFromStore(RequestType.UPDATE_BUDGET_REQ,
                                                  getPortForServer(serverInventory.getPorts(), getServerFromId(customerID)), customerID,
                                                  Integer.toString(budget));
        } else {
            serverInventory.getUserBudgets().put(customerID, serverInventory.getUserBudgets().get(customerID) - price);
        }
    }

    public static int retrieveUserBudget(String customerID, ServerInventory serverInventory, boolean isForeignCustomer) {
        if (isForeignCustomer) {
            String budgetResponse = UDPClientRequestUtil
                    .requestFromStore(RequestType.GET_BUDGET_REQ, getPortForServer(serverInventory.getPorts(), getServerFromId(customerID)),
                                      customerID);
            return Integer.parseInt(budgetResponse);
        } else {
            return serverInventory.getUserBudgets().get(customerID);
        }
    }

}
