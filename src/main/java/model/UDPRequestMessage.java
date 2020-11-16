package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class UDPRequestMessage implements Serializable {

    private final RequestType requestType;
    private final List<String> parameters;
    private final String checksum;

    public UDPRequestMessage(RequestType requestType, List<String> parameters, String checksum) {
        this.requestType = requestType;
        this.parameters = parameters;
        this.checksum = checksum;
    }

    public UDPRequestMessage(RequestType requestType) {
        this(requestType, Collections.emptyList(), "");
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

    @Override
    public String toString() {
        return "requestType=" + requestType + ", parameters=" + parameters;
    }
}
