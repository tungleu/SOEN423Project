package replicaTwo.data.sales;

import java.util.concurrent.ConcurrentHashMap;

public class SalesManagerPool {
    private final static ConcurrentHashMap<String, SalesManager> salesManagers = new ConcurrentHashMap<>();

    public static SalesManager getSalesManagerOnLocation(String managerLocation) {
        salesManagers.computeIfAbsent(managerLocation, k -> new SalesManager());
        return salesManagers.get(managerLocation);
    }

    public static ConcurrentHashMap<String, SalesManager> getManagersPool() {
        return salesManagers;
    }
}
