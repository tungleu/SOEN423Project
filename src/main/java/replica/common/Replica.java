package replica.common;

import model.OperationRequest;
import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;

import java.io.IOException;

import static common.ReplicaConstants.*;
import static util.MessageUtil.messageToUDPRequest;

public abstract class Replica {

    // Message channels
    protected final JChannel sequencerChannel;
    protected final JChannel rmReplicaChannel;
    protected final JChannel replicaClientChannel;

    protected Long sequenceNumber;
    protected RequestExecutor requestExecutor;
    private final String name;

    public Replica(String name) throws Exception {
        this.name = name;
        sequencerChannel = new JChannel().setReceiver(sequenceHandler()).name(name);
        rmReplicaChannel = new JChannel().setReceiver(rmHandler()).name(name);
        // We don't need a handle because it should only be a one-way communication
        replicaClientChannel = new JChannel().name(name);
        sequenceNumber = 0L;
        requestExecutor = new RequestExecutor();
    }

    public Replica start() throws Exception {
        sequencerChannel.connect(SEQUENCER_REPLICA_CLUSTER);
        rmReplicaChannel.connect(REPLICA_RM_CLUSTER);
        replicaClientChannel.connect(CLIENT_REPLICA_CLUSTER);
        return this;
    }

    public void kill() {
        sequencerChannel.disconnect();
        rmReplicaChannel.disconnect();
        replicaClientChannel.disconnect();
    }

    protected abstract void initReplicaStores() throws IOException;

    protected abstract void redirectRequestToStore(OperationRequest operationRequest);

    protected abstract Message handleDataTransferRequest(Address sender, UDPRequestMessage udpRequestMessage);

    private Receiver rmHandler() {
        return msg -> {
            Address src = msg.src();
            UDPRequestMessage udpRequestMessage = messageToUDPRequest(msg);
            try {
                rmReplicaChannel.send(handleDataTransferRequest(src, udpRequestMessage));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private Receiver sequenceHandler() {
        return msg -> {
            OperationRequest operationRequest = (OperationRequest) messageToUDPRequest(msg);
            maybeFetchMissingMessage(msg.src(), operationRequest);
            maybeProcessRequest(operationRequest);
        };
    }

    // TODO(#24): Re-requesting for missing messages
    private void maybeFetchMissingMessage(Address sender, OperationRequest operationRequest) {
    }

    private void maybeProcessRequest(OperationRequest operationRequest) {
        if (operationRequest.getSequenceNumber() == sequenceNumber) {
            redirectRequestToStore(operationRequest);
            sequenceNumber++;
        }
    }
}
