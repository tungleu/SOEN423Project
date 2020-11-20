package replicamanager;

import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Receiver;
import replica.common.Replica;
import util.MessageUtil;
import util.ReplicaUtil;

import java.io.IOException;
import java.util.Stack;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import static common.ReplicaConstants.CLIENT_RM_CLUSTER;
import static common.ReplicaConstants.REPLICA_RM_CLUSTER;
import static model.RequestType.DATA_TRANSFER;
import static util.AddressUtil.fetchAddressForDataTransfer;
import static util.MessageUtil.createMessageFor;

public class ReplicaManager {

    private final String name;
    private String replicaName;
    private final JChannel clientRMChannel;
    private final JChannel rmReplicaChannel;
    private final Stack<Long> failures;

    private Replica replicaServer;
    private final Logger logger;

    public ReplicaManager(String name) throws Exception {
        this.name = name;
        clientRMChannel = new JChannel().setReceiver(clientRequestHandler()).setName(name);
        rmReplicaChannel = new JChannel().setReceiver(replicaRequestHandler()).setName(name);
        failures = new Stack<>();
        logger = Logger.getLogger(name);
    }

    /**
     * Cold start both Replica and Replica Manager's clusters.
     */
    public void coldStart(String replicaOption) throws Exception {
        initLogger();
        replicaName = replicaOption;
        rmReplicaChannel.connect(REPLICA_RM_CLUSTER);

        logger.info(String.format("Starting replica %s.", replicaName));
        replicaServer = ReplicaUtil.startReplica(name, replicaName, null);
        clientRMChannel.connect(CLIENT_RM_CLUSTER);
    }

    private Receiver clientRequestHandler() {
        return msg -> {
            UDPRequestMessage udpRequestMessage = MessageUtil.messageToUDPRequest(msg);
            long failedSequenceNumber = Long.parseLong(udpRequestMessage.getParameters().get(0));
            logger.info(String.format("Received error request from CORBA FE for message with sequence number %d", failedSequenceNumber));

            if (failures.peek() + 1 != failedSequenceNumber) {
                logger.info(String.format("Error message with sequence number %d not consecutive, restarting count.", failedSequenceNumber));
                failures.clear();
            }

            failures.push(failedSequenceNumber);

            // Check if it contains 3 consecutive fails
            if (failures.size() == 3) {
                logger.info(String.format("3 consecutive fails for %s, begin replacement.", replicaName));
                processFailureAndChoseReplica();
            }
        };
    }

    private Receiver replicaRequestHandler() {
        return msg -> {
            try {
                logger.info(String.format("Data received for replacement replica %s, starting replica.", replicaName));
                replicaServer = ReplicaUtil.startReplica(name, replicaName, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private void processFailureAndChoseReplica() {
        // Kill replica
        logger.info(String.format("Killing replica %s.", replicaName));
        replicaServer.kill();

        // Fetch replacement replica
        replicaName = ReplicaUtil.chooseReplica(replicaName);
        logger.info(String.format("Replacing killed replica with replica %s.", replicaName));

        Address destinationReplica = fetchAddressForDataTransfer(name, replicaName, rmReplicaChannel);
        try {
            // Request for data transfer
            logger.info(String.format("Requesting data transfer for replica %s.", replicaName));
            rmReplicaChannel.send(createMessageFor(destinationReplica, new UDPRequestMessage(DATA_TRANSFER)));
            // Clear failures
            failures.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initLogger() throws IOException {
        String logFile = System.getProperty("user.dir") + "/src/main/java/replicamanager/" + name + ".log";
        Handler fileHandler =  new FileHandler(logFile, true);
        this.logger.addHandler(fileHandler);
    }
}
