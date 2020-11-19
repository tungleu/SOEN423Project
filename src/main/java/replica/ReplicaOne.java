package replica;

import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.Message;
import replica.common.Replica;
import replica.data.ReplicaOneData;
import replicaOne.model.Pair;
import replicaOne.model.ServerInventory;
import replicaOne.server.impl.Store;
import replicaOne.server.util.data.UserDataLoader;
import util.MessageUtil;

import static common.ReplicaConstants.*;
import static replicaOne.server.util.udp.UDPClientRequestUtil.*;
import static replicaOne.server.util.udp.UDPServerStarterUtil.startUDPServer;

public class ReplicaOne extends Replica {

    private final static Pair[] SERVERS = new Pair[] {
      new Pair(QC_SERVER_NAME, QC_PORT),
      new Pair(BC_SERVER_NAME, BC_PORT),
      new Pair(ON_SERVER_NAME, ON_PORT)
    };

    private final ReplicaOneData replicaOneData;

    public ReplicaOne(String name, ReplicaOneData replicaOneData) throws Exception {
        super(name);
        this.replicaOneData = replicaOneData;
    }

    @Override
    protected void initReplicaStores() {
        for (Pair<String, Integer> serverPair: SERVERS) {
            String serverName = serverPair.getKey();
            int port = serverPair.getValue();

            ServerInventory serverInventory = fetchServerInventory(serverName);
            UserDataLoader.loadData(serverName, serverInventory);
            Store store = new Store(serverName, port, serverInventory);
            storeMap.put(serverName, store);

            startUDPServer(port, serverInventory);
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
