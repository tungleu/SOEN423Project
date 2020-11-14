package replicaTwo.data.sales;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class SalesManagerPool implements Serializable {
    private final ConcurrentHashMap<String, SalesManager> salesManagers;

    public SalesManagerPool() {
        this.salesManagers = new ConcurrentHashMap<>();
    }

    public SalesManagerPool(SalesManagerPool managerPool) {
        this.salesManagers = new ConcurrentHashMap<>(managerPool.getSalesManagers());
    }

    public SalesManager getSalesManagerOnLocation(String managerLocation) {
        salesManagers.computeIfAbsent(managerLocation, k -> new SalesManager());
        return salesManagers.get(managerLocation);
    }

    public ConcurrentHashMap<String, SalesManager> getSalesManagers() {
        return this.salesManagers;
    }

}
