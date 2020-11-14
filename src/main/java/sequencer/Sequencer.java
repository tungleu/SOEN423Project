package sequencer;

import model.OperationRequest;
import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import util.MessageUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import static common.ReplicaConstants.*;
import static util.MessageUtil.messageToUDPRequest;

public class Sequencer {
    private final JChannel clientChannel;
    private final JChannel replicaChannel;
    private final AtomicLong sequenceNumber;
    private final Map<Long, UDPRequestMessage> messageHistory;
    private final Logger logger;

    public Sequencer(String name) throws Exception {
        super();
        this.clientChannel = new JChannel().setReceiver(clientHandler()).name(name);
        this.replicaChannel = new JChannel().setDiscardOwnMessages(true).setReceiver(replicaHandler()).name(name);
        this.sequenceNumber = new AtomicLong();
        this.messageHistory = new ConcurrentHashMap<>();
        this.logger = Logger.getLogger(name);
    }

    public void start() throws Exception {
        this.clientChannel.connect(CLIENT_SEQUENCER_CLUSTER);
        this.replicaChannel.connect(SEQUENCER_REPLICA_CLUSTER);
    }

    private Receiver replicaHandler() {
        return msg -> {
            UDPRequestMessage requestMessage = messageToUDPRequest(msg);
            Address src = msg.src();
            long missingSeqNum = Long.parseLong(requestMessage.getParameters().get(0));
            UDPRequestMessage missingOperation = this.messageHistory.get(missingSeqNum);

            try {
                this.replicaChannel.send(handleReplicaMessage(src, missingOperation));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private Receiver clientHandler() {
        return msg -> {
            OperationRequest operationRequest = (OperationRequest) messageToUDPRequest(msg);
            this.processNewMessage(operationRequest);

            try {
                this.replicaChannel.send(handleReplicaMessage(null, operationRequest));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    // set dst -> null to multicast
    private Message handleReplicaMessage(@Nullable Address dst, UDPRequestMessage operationRequest) {
        return MessageUtil.createMessageFor(dst, operationRequest);
    }

    private void processNewMessage(OperationRequest operationRequest) {
        operationRequest.setSequenceNumber(sequenceNumber.get());
        this.messageHistory.putIfAbsent(sequenceNumber.getAndIncrement(), operationRequest);
    }

    public static void main(String[] args) throws Exception {
        Sequencer sequencer = new Sequencer(SEQUENCER_NAME);
        sequencer.initLogger();
        sequencer.start();
        System.out.println("Sequencer is online.");
    }

    public void initLogger() throws IOException {
        String logFile = SEQUENCER_NAME + ".log";
        Handler fileHandler  = new FileHandler(System.getProperty("user.dir") + "/src/main/java/sequencer/" + logFile, true);
        this.logger.setUseParentHandlers(false);
        this.logger.addHandler(fileHandler);
    }
}
