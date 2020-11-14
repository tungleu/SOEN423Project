package model;

import java.io.Serializable;

public class UDPResponseMessage implements Serializable {

    private final String sender;
    private final String responseType;
    private final Serializable response;
    private final String checksum;

    public UDPResponseMessage(String sender, String responseType, Serializable response, String checksum) {
        this.sender = sender;
        this.responseType = responseType;
        this.response = response;
        this.checksum = checksum;
    }

    public String getSender() {
        return sender;
    }

    public String getResponseType() {
        return responseType;
    }

    public Serializable getResponse() {
        return response;
    }

    public String getChecksum() {
        return checksum;
    }
}
