package model;

import java.util.List;

public class OperationRequest extends UDPRequestMessage {

    private final String corbaClient;
    private long sequenceNumber;

    public OperationRequest(RequestType requestType, List<String> parameters, String checksum, String corbaClient) {
        super(requestType, parameters, checksum);
        this.corbaClient = corbaClient;
        this.sequenceNumber = 0;
    }

    public String getCorbaClient() {
        return corbaClient;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String toString() {
        return super.toString() + ", corbaClient='" + corbaClient + ", sequenceNumber=" + sequenceNumber;
    }
}
