package replicamanager;

import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Receiver;
import replica.common.Replica;
import util.MessageUtil;
import util.ReplicaUtil;

import java.util.Stack;

import static common.ReplicaConstants.CLIENT_RM_CLUSTER;
import static common.ReplicaConstants.REPLICA_RM_CLUSTER;
import static model.RequestType.DATA_TRANSFER;
import static util.MessageUtil.createMessageFor;
import static util.ReplicaUtil.fetchAddressForDataTransfer;

public class ReplicaManager {

    private final String name;
    private String replicaName;
    private final JChannel clientRMChannel;
    private final JChannel rmReplicaChannel;
    private final Stack<Long> failures;

    private Replica replicaServer;

    public ReplicaManager(String name) throws Exception {
        this.name = name;
        clientRMChannel = new JChannel().setReceiver(clientRequestHandler()).setName(name);
        rmReplicaChannel = new JChannel().setReceiver(replicaRequestHandler()).setName(name);
        failures = new Stack<>();
    }

    /**
     * Cold start both Replica and Replica Manager's clusters.
     */
    public void coldStart(String replicaOption) throws Exception {
        replicaName = replicaOption;
        clientRMChannel.connect(REPLICA_RM_CLUSTER);
        replicaServer = ReplicaUtil.startReplica(name, replicaName, null);
        clientRMChannel.connect(CLIENT_RM_CLUSTER);
    }

    private Receiver clientRequestHandler() {
        return msg -> {
            UDPRequestMessage udpRequestMessage = MessageUtil.messageToUDPRequest(msg);
            long failedSequenceNumber = Long.parseLong(udpRequestMessage.getParameters().get(0));

            if (failures.peek() + 1 != failedSequenceNumber) {
                failures.clear();
            }

            failures.push(failedSequenceNumber);

            // Check if it contains 3 consecutive fails
            if (failures.size() == 3) {
                processFailureAndChoseReplica();
            }
        };
    }

    private Receiver replicaRequestHandler() {
        return msg -> {
            try {
                replicaServer = ReplicaUtil.startReplica(name, replicaName, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private void processFailureAndChoseReplica() {
        // Kill replica
        replicaServer.kill();
        // Fetch replacement replica
        replicaName = ReplicaUtil.chooseReplica(replicaName);
        Address destinationReplica = fetchAddressForDataTransfer(name, replicaName, rmReplicaChannel);
        try {
            // Request for data transfer
            rmReplicaChannel.send(createMessageFor(destinationReplica, new UDPRequestMessage(DATA_TRANSFER)));
            // Clear failures
            failures.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
