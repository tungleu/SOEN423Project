package replica.data;

import replicaOne.model.ServerInventory;

import java.io.Serializable;

public class ReplicaOneData implements Serializable {

    private final ServerInventory bcInventory;
    private final ServerInventory qcInventory;
    private final ServerInventory onInventory;

    public ReplicaOneData(ServerInventory bcInventory, ServerInventory qcInventory, ServerInventory onInventory) {
        this.bcInventory = bcInventory;
        this.qcInventory = qcInventory;
        this.onInventory = onInventory;
    }

    public ServerInventory getBcInventory() {
        return bcInventory;
    }

    public ServerInventory getQcInventory() {
        return qcInventory;
    }

    public ServerInventory getOnInventory() {
        return onInventory;
    }
}
