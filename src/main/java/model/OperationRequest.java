package model;

import java.util.List;

public class OperationRequest extends UDPRequestMessage {

    private final String corbaClient;
    private final Long sequenceNumber;

    public OperationRequest(RequestType requestType, List<String> parameters, String checksum, String pid, String corbaClient,
                            Long sequenceNumber) {
        super(requestType, parameters, checksum, pid);
        this.corbaClient = corbaClient;
        this.sequenceNumber = sequenceNumber;
    }

    public String getCorbaClient() {
        return corbaClient;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }
}
