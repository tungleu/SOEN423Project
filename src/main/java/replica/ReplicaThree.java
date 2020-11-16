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

    public ReplicaThree(String name, ReplicaThreeData replicaThreeData) throws Exception {
        super(name);
        this.replicaThreeData = replicaThreeData;
    }

    @Override
    protected void initReplicaStores() throws IOException {

        Store BCstore = new Store(replicaThreeData.getBCData());
        Store ONstore = new Store(replicaThreeData.getONData());
        Store QCstore = new Store(replicaThreeData.getQCData());

        this.storeMap.put(BC_SERVER_NAME, BCstore);
        this.storeMap.put(ON_SERVER_NAME, ONstore);
        this.storeMap.put(QC_SERVER_NAME, QCstore);

        Runnable task1 = BCstore::receive;
        Runnable task2 = ONstore::receive;
        Runnable task3 = QCstore::receive;

        Thread thread1 = new Thread(task1);
        Thread thread2 = new Thread(task2);
        Thread thread3 = new Thread(task3);

        thread1.start();
        thread2.start();
        thread3.start();

    }

    @Override
    protected Message handleDataTransferRequest(Address sender, UDPRequestMessage udpRequestMessage) {
        return MessageUtil.createMessageFor(sender, this.replicaThreeData);
    }
}