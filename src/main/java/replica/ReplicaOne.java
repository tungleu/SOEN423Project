package replica;

import common.StoreStrategy;
import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.Message;
import replica.common.Replica;
import replica.data.ReplicaOneData;
import util.MessageUtil;

public class ReplicaOne extends Replica {

    private final ReplicaOneData replicaOneData;

    public ReplicaOne(String name, ReplicaOneData replicaOneData) throws Exception {
        super(name);
        this.replicaOneData = replicaOneData;
        initReplicaStores();
    }

    @Override
    protected void initReplicaStores() {
        // TODO(#22): Integrate Kevin's codebase
    }

    @Override
    protected StoreStrategy fetchStore(String targetStore) {
        // TODO(#23): Handling Sequencer requests for Kevin's codebase
        return null;
    }

    @Override
    protected Message handleDataTransferRequest(Address sender, UDPRequestMessage udpRequestMessage) {
        return MessageUtil.createMessageFor(sender, replicaOneData);
    }
}
