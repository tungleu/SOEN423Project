package replica;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.Message;
import replica.common.Replica;
import replica.data.ReplicaTwoData;
import replicaTwo.store.StoreProxy;
import util.MessageUtil;

import java.io.IOException;
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
        Map<String, Integer> portsConfig = ImmutableMap.<String, Integer> builder()
                .put(QC_SERVER_NAME, 8887)
                .put(BC_SERVER_NAME, 8888)
                .put(ON_SERVER_NAME, 8889)
                .build();;
        for(String serverName : SERVER_NAMES) {
            StoreProxy storeProxy = new StoreProxy(serverName, this.replicaTwoData, portsConfig);
            this.storeMap.put(serverName, storeProxy);
            storeProxy.initializeStore(portsConfig.get(serverName));
        }
    }

    @Override
    protected Message handleDataTransferRequest(Address sender, UDPRequestMessage udpRequestMessage) {
        return MessageUtil.createMessageFor(sender, this.replicaTwoData);
    }
}
