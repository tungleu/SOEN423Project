package replica;

import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.Message;
import replica.common.Replica;
import replica.data.ReplicaThreeData;
import replicaThree.store.Store;
import util.MessageUtil;

import java.io.IOException;

import static common.ReplicaConstants.*;

public class ReplicaThree extends Replica {

    private final ReplicaThreeData replicaThreeData;
    private boolean isFreshStart;
    public ReplicaThree(String name, ReplicaThreeData replicaThreeData, boolean isFreshStart) throws Exception {
        super(name);
        this.replicaThreeData = replicaThreeData;
        this.isFreshStart = isFreshStart;
    }

    @Override
    protected void initReplicaStores() throws IOException {
        if(!this.isFreshStart){
            replicaThreeData.resetPorts();
        }
        Store BCstore = new Store(replicaThreeData.getBCData());
        Store ONstore = new Store(replicaThreeData.getONData());
        Store QCstore = new Store(replicaThreeData.getQCData());

        this.storeMap.put(BC_SERVER_NAME, BCstore);
        this.storeMap.put(ON_SERVER_NAME, ONstore);
        this.storeMap.put(QC_SERVER_NAME, QCstore);

        BCstore.startUDPServer(isRunning);
        ONstore.startUDPServer(isRunning);
        QCstore.startUDPServer(isRunning);

    }

    @Override
    protected Message handleDataTransferRequest(Address sender, UDPRequestMessage udpRequestMessage) {
        return MessageUtil.createMessageFor(sender, this.replicaThreeData);
    }
}
