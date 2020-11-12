package replica.common;

import model.OperationRequest;
import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;

import static common.ReplicaConstants.REPLACE_RM_CLUSTER;
import static common.ReplicaConstants.SEQUENCER_REPLICA_CLUSTER;
import static util.MessageUtil.messageToUDPRequest;

public abstract class Replica {

    // Message channels
    protected final JChannel sequencerChannel;
    protected final JChannel rmReplicaChannel;

    protected Long sequenceNumber;
    private final String name;

    public Replica(String name) throws Exception {
        this.name = name;
        sequencerChannel = new JChannel().setReceiver(sequenceHandler()).name(name);
        rmReplicaChannel = new JChannel().setReceiver(rmHandler()).name(name);
        sequenceNumber = 0L;
    }

    public Replica start() throws Exception {
        sequencerChannel.connect(SEQUENCER_REPLICA_CLUSTER);
        rmReplicaChannel.connect(REPLACE_RM_CLUSTER);
        return this;
    }

    public void kill() {
        sequencerChannel.disconnect();
        rmReplicaChannel.disconnect();
    }

    protected abstract void initReplicaStores();

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
        if (operationRequest.getSequenceNumber().longValue() == sequenceNumber) {
            redirectRequestToStore(operationRequest);
            sequenceNumber++;
        }
    }
}
