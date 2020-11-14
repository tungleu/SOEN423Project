package replica;

import common.StoreStrategy;
import model.OperationRequest;
import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.Message;
import replica.common.Replica;
import replica.data.ReplicaTwoData;
import replicaTwo.store.StoreProxy;
import util.MessageUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static common.ReplicaConstants.*;
import static java.util.Map.entry;

public class ReplicaTwo extends Replica  {
    private final ReplicaTwoData replicaTwoData;
    private final Map<String, StoreProxy> stores;

    public ReplicaTwo(String name, ReplicaTwoData replicaTwoData) throws Exception {
        super(name);
        this.replicaTwoData = replicaTwoData;
        this.stores = new HashMap<>();
        initReplicaStores();
    }

    @Override
    protected void initReplicaStores() throws IOException {
        Map<String, Integer> portsConfig = Map.ofEntries(
                entry(QC_SERVER_NAME, 8887),
                entry(BC_SERVER_NAME, 8888),
                entry(ON_SERVER_NAME, 8889)
        );
        for(String serverName : SERVER_NAMES) {
            StoreProxy storeProxy = new StoreProxy(serverName, this.replicaTwoData, portsConfig);
            this.stores.put(serverName, storeProxy);
            storeProxy.initializeStore(portsConfig.get(serverName));
        }
    }

    @Override
    protected void redirectRequestToStore(OperationRequest operationRequest) {
        String targetStore = MessageUtil.fetchTargetStore(operationRequest);
        StoreStrategy store = this.stores.get(targetStore);
        this.requestExecutor.execute(store, operationRequest);
    }

    @Override
    protected Message handleDataTransferRequest(Address sender, UDPRequestMessage udpRequestMessage) {
        return MessageUtil.createMessageFor(sender, this.replicaTwoData);
    }
}
