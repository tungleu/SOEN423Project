package model;

import java.io.Serializable;
import java.util.List;

public class UDPRequestMessage implements Serializable {

    private final RequestType requestType;
    private final List<String> parameters;
    private final String checksum;
    private final String pid;

    public UDPRequestMessage(RequestType requestType, List<String> parameters, String checksum, String pid) {
        this.requestType = requestType;
        this.parameters = parameters;
        this.checksum = checksum;
        this.pid = pid;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getPid() {
        return pid;
    }
}
