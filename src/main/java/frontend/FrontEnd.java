package frontend;
import CORBA_FE.FrontEndPOA;
import common.ReplicaConstants;
import model.OperationRequest;
import model.RequestType;
import model.UDPRequestMessage;
import model.UDPResponseMessage;
import org.jgroups.Address;
import org.jgroups.Receiver;
import org.omg.CORBA.ORB;
import org.jgroups.JChannel;
import util.AddressUtil;
import util.ChecksumUtil;
import util.MessageUtil;
import util.ReplicaUtil;
import java.util.*;


public class FrontEnd extends FrontEndPOA{
    private final JChannel clientSequencerChannel;
    private final JChannel clientRMChannel;
    private final JChannel clientReplicaChannel;
    private final ORB orb;
    private final String CORBA_CLIENT_NAME;
    private final Map<String, Response> responseMap;

    public FrontEnd(String name, ORB orb, Map<String, Response> responseMap) throws Exception {
        this.orb = orb;
        this.CORBA_CLIENT_NAME = name;
        this.responseMap = responseMap;
        clientSequencerChannel = new JChannel().setName(CORBA_CLIENT_NAME);
        clientReplicaChannel = new JChannel().setReceiver(replicaResponseHandler()).setName(CORBA_CLIENT_NAME);
        clientRMChannel = new JChannel().setName(CORBA_CLIENT_NAME);
        this.connectClusters();

    }
    @Override
    public String addItem(String managerID, String itemID, String itemName, int quantity, int price) {
        List<String> params = Arrays.asList(managerID, itemID, itemName,  Integer.toString(quantity), Integer.toString(price));
        return marshallRequest(ChecksumUtil.generateChecksumSHA256(params.toString()), RequestType.ADD_ITEM, params);
    }

    @Override
    public String removeItem(String managerID, String itemID, int quantity) {
        List<String> params = Arrays.asList(managerID, itemID, Integer.toString(quantity));
        return marshallRequest(ChecksumUtil.generateChecksumSHA256(params.toString()), RequestType.REMOVE_ITEM, params);
    }

    @Override
    public String listItemAvailability(String managerID) {
        List<String> params = Arrays.asList(managerID);
        return marshallRequest(ChecksumUtil.generateChecksumSHA256(params.toString()), RequestType.LIST_ITEM_AVAILABILITY, params);
    }

    @Override
    public String purchaseItem(String customerID, String itemID, String dateOfPurchase) {
        List<String> params = Arrays.asList(customerID, itemID, dateOfPurchase);
        return marshallRequest(ChecksumUtil.generateChecksumSHA256(params.toString()), RequestType.PURCHASE_ITEM, params);
    }

    @Override
    public String findItem(String customerID, String itemName) {
        List<String> params = Arrays.asList(customerID, itemName);
        return marshallRequest(ChecksumUtil.generateChecksumSHA256(params.toString()), RequestType.FIND_ITEM, params);
    }

    @Override
    public String returnItem(String customerID, String itemID, String dateOfReturn) {
        List<String> params = Arrays.asList(customerID, itemID, dateOfReturn);
        return marshallRequest(ChecksumUtil.generateChecksumSHA256(params.toString()), RequestType.RETURN_ITEM, params);
    }

    @Override
    public String exchangeItem(String customerID, String newitemID, String oldItemID, String dateOfExchange) {
        List<String> params = Arrays.asList(customerID, newitemID, oldItemID, dateOfExchange);
        return marshallRequest(ChecksumUtil.generateChecksumSHA256(params.toString()), RequestType.EXCHANGE_ITEM, params);
    }

    @Override
    public String addWaitList(String customerID, String itemID) {
        List<String> params = Arrays.asList(customerID, itemID);
        return marshallRequest(ChecksumUtil.generateChecksumSHA256(params.toString()), RequestType.ADD_WAIT_LIST, params);
    }

    @Override
    public String killReplica(int replica) {
        return null;
    }


    private String marshallRequest(String checksum, RequestType requestType, List<String> params){
        OperationRequest operationRequest = new OperationRequest(requestType, params, checksum, CORBA_CLIENT_NAME);
        Response response = new Response();
        responseMap.put(checksum, response);
        try {
            clientSequencerChannel.send(MessageUtil.createMessageFor(null, operationRequest));
            while(new Date().getTime() - response.getInitialTime().getTime() < 5000 && response.getResponses().size() != 3){}
            if(!response.isEqual() || response.getResponses().size() != 3){
                this.handleFailure(response);
            }
            return response.getFinalResponse();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";

    }

    private void handleFailure(Response response){
        String replicaManager = "";
        Map<String, String> responseMap = response.getResponseMap();
        if(response.getResponses().size() != 3){
           for(String name : ReplicaConstants.REPLICA_MANAGER_NAMES){
               if(!responseMap.containsKey(name)){
                   replicaManager = name;
               }
           }
        }
        else {
            String responseOne = responseMap.get(ReplicaConstants.REPLICA_MANAGER_ONE);
            if(responseOne.equals(responseMap.get(ReplicaConstants.REPLICA_MANAGER_TWO))){
                replicaManager = ReplicaConstants.REPLICA_MANAGER_THREE;
            }
            else if (response.equals(responseMap.get(ReplicaConstants.REPLICA_MANAGER_THREE))){
                replicaManager = ReplicaConstants.REPLICA_MANAGER_TWO;
            }
            else{
                replicaManager = ReplicaConstants.REPLICA_MANAGER_ONE;
            }
        }
        System.out.println(replicaManager);
        this.sendFailureMessage(response.getSequenceNumber(), replicaManager);



    }
    private void sendFailureMessage(long sequenceNumber, String replicaManagerName){
        try {
            List<String> sequencerParam = Collections.singletonList(Long.toString(sequenceNumber));
            UDPRequestMessage udpRequestMessage = new UDPRequestMessage(RequestType.FAILURE, sequencerParam, "" );
            Address replicaManagerAddress = AddressUtil.findAddressForGivenName(clientRMChannel, replicaManagerName);
            clientRMChannel.send(MessageUtil.createMessageFor(replicaManagerAddress,udpRequestMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Receiver replicaResponseHandler(){
        return msg -> {
            UDPResponseMessage udpResponseMessage = msg.getObject(UDPResponseMessage.class.getClassLoader());
            System.out.println(udpResponseMessage.getResponse());
            Response response = responseMap.get(udpResponseMessage.getChecksum());
            response.setSequenceNumber(udpResponseMessage.getSequenceNumber());
            response.addResponse(ReplicaUtil.fetchRMNamefromReplica(udpResponseMessage.getSender()),(String)udpResponseMessage.getResponse());
        };
    }
    private void connectClusters(){
        try {
            this.clientRMChannel.connect(ReplicaConstants.CLIENT_RM_CLUSTER);
            this.clientSequencerChannel.connect(ReplicaConstants.CLIENT_SEQUENCER_CLUSTER);
            this.clientReplicaChannel.connect(ReplicaConstants.CLIENT_REPLICA_CLUSTER);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
