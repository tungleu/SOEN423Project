package replicaOne.server.util.data;

import replicaOne.model.ServerInventory;

import java.util.Map;

public final class UserDataLoader {

    private UserDataLoader() {
    }

    public static void loadData(String server, ServerInventory serverInventory) {
        Map<String, Integer> budgets = serverInventory.getUserBudgets();
        for (int i = 0; i < 3; i++) {
            int id = 1000 + i;
            String userId = String.format(server + "U%d", id);
            budgets.put(userId, 1000);
        }
    }

}
