package sequencer;

import model.OperationRequest;
import model.UDPRequestMessage;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import util.MessageUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import static common.ReplicaConstants.*;
import static util.MessageUtil.messageToUDPRequest;

public class Sequencer {
    private static final String SEQUENCER_LOG_MSG = "Received request from the Client %s to sequence %s operation";
    private final JChannel clientChannel;
    private final JChannel replicaChannel;
    private final AtomicLong sequenceNumber;
    private final Logger logger;

    public Sequencer(String name) throws Exception {
        super();
        this.clientChannel = new JChannel().setReceiver(clientHandler()).name(name);
        this.replicaChannel = new JChannel().name(name);
        this.sequenceNumber = new AtomicLong();
        this.logger = Logger.getLogger(name);
    }

    public void start() throws Exception {
        this.clientChannel.connect(CLIENT_SEQUENCER_CLUSTER);
        this.replicaChannel.connect(SEQUENCER_REPLICA_CLUSTER);
    }

    private Receiver clientHandler() {
        return msg -> {
            OperationRequest operationRequest = (OperationRequest) messageToUDPRequest(msg);
            operationRequest.setSequenceNumber(sequenceNumber.getAndIncrement());
            this.logger.info(String.format(SEQUENCER_LOG_MSG, operationRequest.getCorbaClient(), operationRequest.getRequestType().name()));
            try {
                this.replicaChannel.send(handleReplicaMessage(operationRequest));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private Message handleReplicaMessage(UDPRequestMessage operationRequest) {
        return MessageUtil.createMessageFor(null, operationRequest);
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
