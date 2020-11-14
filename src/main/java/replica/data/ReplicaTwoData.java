package replica.data;

import com.rits.cloning.Cloner;
import replicaTwo.data.inventory.StoreInventoryPool;
import replicaTwo.data.inventory.StoreInventory;
import replicaTwo.data.sales.SalesManager;
import replicaTwo.data.sales.SalesManagerPool;
import util.CloneUtil;

import java.io.Serializable;

public class ReplicaTwoData implements Serializable {
    private final StoreInventoryPool inventories;
    private final SalesManagerPool salesManagers;

    public ReplicaTwoData() {
        this.inventories = new StoreInventoryPool();
        this.salesManagers = new SalesManagerPool();
    }

    public ReplicaTwoData(ReplicaTwoData replicaTwoData) {
        Cloner cloner = CloneUtil.getCloner();
        this.inventories = new StoreInventoryPool(cloner.deepClone(replicaTwoData.getInventoryPool()));
        this.salesManagers = new SalesManagerPool(cloner.deepClone(replicaTwoData.getSalesManagerPool()));
    }

    public SalesManager getSalesManagerOnLocation(String location) {
        return this.salesManagers.getSalesManagerOnLocation(location);
    }

    public StoreInventory getInventoryOnLocation(String location) {
        return this.inventories.getInventoryOnLocation(location);
    }

    public StoreInventoryPool getInventoryPool() {
        return this.inventories;
    }

    public SalesManagerPool getSalesManagerPool() {
        return this.salesManagers;
    }
}
