package replica;

import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.Message;
import replica.common.Replica;
import replica.data.ReplicaOneData;
import replicaOne.model.ServerInventory;
import replicaOne.server.impl.Store;
import replicaOne.server.util.data.UserDataLoader;
import util.MessageUtil;

import static common.ReplicaConstants.*;
import static replicaOne.server.util.udp.UDPClientRequestUtil.*;
import static replicaOne.server.util.udp.UDPServerStarterUtil.startUDPServer;

public class ReplicaOne extends Replica {

    private final ReplicaOneData replicaOneData;
    private boolean isFreshStart;

    public ReplicaOne(String name, ReplicaOneData replicaOneData, boolean isFreshStart) throws Exception {
        super(name);
        this.replicaOneData = replicaOneData;
        this.isFreshStart = isFreshStart;
    }

    @Override
    protected void initReplicaStores() {
        int[] ports = new int[]{QC_PORT, BC_PORT, ON_PORT};
        if (!isFreshStart) {
            for (int i = 0; i < ports.length; i++) {
                ports[i] += ports.length;
            }
        }
        for (int i = 0; i < SERVER_NAMES.length; i++) {
            String serverName = SERVER_NAMES[i];

            ServerInventory serverInventory = fetchServerInventory(serverName);
            serverInventory.setPorts(ports);

            UserDataLoader.loadData(serverName, serverInventory);
            Store store = new Store(serverName, i, serverInventory);
            storeMap.put(serverName, store);

            startUDPServer(ports[i], serverInventory);
        }
    }

    @Override
    protected Message handleDataTransferRequest(Address sender, UDPRequestMessage udpRequestMessage) {
        return MessageUtil.createMessageFor(sender, replicaOneData);
    }

    private ServerInventory fetchServerInventory(String serverName) {
        switch (serverName) {
            case QC_SERVER_NAME:
                return replicaOneData.getQcInventory();
            case BC_SERVER_NAME:
                return replicaOneData.getBcInventory();
            default:
                return replicaOneData.getOnInventory();
        }
    }
}
