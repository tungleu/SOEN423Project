package replica.common;

import common.StoreStrategy;
import model.OperationRequest;
import model.UDPRequestMessage;
import model.UDPResponseMessage;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import util.AddressUtil;
import util.MessageUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import static common.OperationResponse.STRING_RESPONSE_TYPE;
import static common.ReplicaConstants.*;
import static util.MessageUtil.createMessageFor;
import static util.MessageUtil.messageToUDPRequest;

public abstract class Replica {

    // Message channels
    protected final JChannel sequencerChannel;
    protected final JChannel rmReplicaChannel;
    protected final JChannel replicaClientChannel;

    protected final Map<String, StoreStrategy> storeMap;

    private final String name;
    private final Logger logger;

    public Replica(String name) throws Exception {
        this.name = name;
        sequencerChannel = new JChannel().setReceiver(sequenceHandler()).name(name);
        rmReplicaChannel = new JChannel().setReceiver(rmHandler()).name(name);
        // We don't need a handle because it should only be a one-way communication
        replicaClientChannel = new JChannel().name(name);
        storeMap = new HashMap<>();
        logger = Logger.getLogger(name);
    }

    public Replica start() throws Exception {
        initLogger();
        initReplicaStores();
        sequencerChannel.connect(SEQUENCER_REPLICA_CLUSTER);
        rmReplicaChannel.connect(REPLICA_RM_CLUSTER);
        replicaClientChannel.connect(CLIENT_REPLICA_CLUSTER);
        logger.info(String.format(LOG_REPLICA_START, name));
        return this;
    }

    public void kill() {
        sequencerChannel.disconnect();
        rmReplicaChannel.disconnect();
        replicaClientChannel.disconnect();
        logger.info(String.format(LOG_REPLICA_KILL, name));
    }

    protected abstract void initReplicaStores() throws IOException;

    protected abstract Message handleDataTransferRequest(Address sender, UDPRequestMessage udpRequestMessage);

    private void redirectRequestToStore(OperationRequest operationRequest) {
        logger.info(String.format(LOG_REPLICA_REQUEST_RECEIVED, name, operationRequest.toString()));
        List<String> params = operationRequest.getParameters();
        StoreStrategy store = storeMap.get(MessageUtil.fetchTargetStore(operationRequest));

        String response;
        try {
            switch (operationRequest.getRequestType()) {
                case ADD_ITEM:
                    response = store.addItem(params.get(0), params.get(1), params.get(2), Integer.parseInt(params.get(3)),
                                             Integer.parseInt(params.get(4)));
                    break;
                case REMOVE_ITEM:
                    response = store.removeItem(params.get(0), params.get(1), Integer.parseInt(params.get(2)));
                    break;
                case LIST_ITEM_AVAILABILITY:
                    response = store.listItemAvailability(params.get(0));
                    break;
                case PURCHASE_ITEM:
                    response = store.purchaseItem(params.get(0), params.get(1), params.get(2));
                    break;
                case FIND_ITEM:
                    response = store.findItem(params.get(0), params.get(1));
                    break;
                case RETURN_ITEM:
                    response = store.returnItem(params.get(0), params.get(1), params.get(2));
                    break;
                case EXCHANGE_ITEM:
                    response = store.exchangeItem(params.get(0), params.get(1), params.get(2), params.get(3));
                    break;
                case ADD_WAIT_LIST:
                    response = store.addWaitList(params.get(0), params.get(1));
                    break;
                default:
                    response = "Unknown operation request";
            }
        } catch (Exception e) {
            response = e.getMessage();
        }

        logger.info(String.format(LOG_REPLICA_REQUEST_RESULT, name, operationRequest.getSequenceNumber(), response));
        sendResponseToClient(operationRequest, response);
    }

    private Receiver rmHandler() {
        return msg -> {
            logger.info(String.format(LOG_REPLICA_DATA_TRANSFER_REQUEST, name));
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
            redirectRequestToStore(operationRequest);
        };
    }

    private void sendResponseToClient(OperationRequest operationRequest, String operationResponse) {
        Address clientAddress = AddressUtil.findAddressForGivenName(replicaClientChannel, operationRequest.getCorbaClient());
        UDPResponseMessage response = new UDPResponseMessage(name, STRING_RESPONSE_TYPE, operationResponse, operationRequest.getChecksum(), operationRequest.getSequenceNumber());
        try {
            replicaClientChannel.send(createMessageFor(clientAddress, response));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initLogger() throws IOException {
        String logFile = System.getProperty("user.dir") + "/src/main/java/replica/common/" + name + ".log";
        Handler fileHandler = new FileHandler(logFile, true);
        this.logger.addHandler(fileHandler);
    }
}
