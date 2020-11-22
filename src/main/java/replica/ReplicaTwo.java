package replica;

import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.Message;
import replica.common.Replica;
import replica.data.ReplicaTwoData;
import replicaTwo.store.StoreProxy;
import util.MessageUtil;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

import static common.ReplicaConstants.*;


public class ReplicaTwo extends Replica  {

    private final ReplicaTwoData replicaTwoData;

    public ReplicaTwo(String name, ReplicaTwoData replicaTwoData) throws Exception {
        super(name);
        this.replicaTwoData = replicaTwoData;
    }

    @Override
    protected void initReplicaStores() throws IOException {
        Map<String, DatagramSocket> socketConfig = new HashMap<>();
        for(String serverName : SERVER_NAMES) {
            DatagramSocket socket = new DatagramSocket(null);
            socket.bind(null);
            socketConfig.put(serverName, socket);
            udpServers.add(socket);
        }
        for(String serverName : SERVER_NAMES) {
            StoreProxy storeProxy = new StoreProxy(serverName, this.replicaTwoData, socketConfig);
            this.storeMap.put(serverName, storeProxy);
            storeProxy.initializeStore(socketConfig.get(serverName));
        }
    }

    @Override
    protected Message handleDataTransferRequest(Address sender, UDPRequestMessage udpRequestMessage) {
        return MessageUtil.createMessageFor(sender, this.replicaTwoData);
    }
}
