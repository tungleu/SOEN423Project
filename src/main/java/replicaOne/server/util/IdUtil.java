package replicaOne.server.util;


import replicaOne.model.ServerInventory;

/**
 * Created by Kevin Tan 2020-09-20
 */
public final class IdUtil {

    private static final int ID_INDEX = 2;
    private static final int ID_NUMBER_INDEX = 3;
    private static final String MANAGER_ID_NUMBER = "1001";
    private static final char MANAGER_ID = 'M';
    private static final char CUSTOMER_ID = 'U';
    private static final String UNAUTHORIZED_ACTION =
            "User %s does not have permission, please use an appropriate account to perform this action.";
    private static final String USER_NOT_FOUND =
            "User %s does not exist in this store, please use an appropriate account to perform this action.";

    private IdUtil() {
    }

    public static void checkHasAccess(ServerInventory serverInventory, String id, boolean isManager) throws Exception {
        if (!isManager && !serverInventory.getUserBudgets().containsKey(id)) {
            throw new Exception(String.format(USER_NOT_FOUND, id));
        }
        if ((isManager && !isManager(id)) || (!isManager && !isCustomer(id))) {
            throw new Exception(String.format(UNAUTHORIZED_ACTION, id));
        }
    }

    public static String getServerFromId(String id) {
        return id.substring(0, 2);
    }

    private static boolean isManager(String id) {
        return parseId(id) == MANAGER_ID && id.substring(ID_NUMBER_INDEX).equals(MANAGER_ID_NUMBER);
    }

    private static boolean isCustomer(String id) {
        return parseId(id) == CUSTOMER_ID;
    }

    private static Character parseId(String id) {
        return id.charAt(ID_INDEX);
    }
}
