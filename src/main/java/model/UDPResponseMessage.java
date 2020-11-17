package model;

import util.ChecksumUtil;

import java.io.Serializable;

public class UDPResponseMessage implements Serializable {

    private final String sender;
    private final String responseType;
    private Serializable response;
    private long sequenceNumber;
    private final String checksum;

    public UDPResponseMessage(String sender, String responseType, Serializable response, String checksum) {
        this.sender = sender;
        this.responseType = responseType;
        this.response = response;
        this.checksum = checksum;
    }

    public UDPResponseMessage(String sender, String responseType) {
        this.sender = sender;
        this.responseType = responseType;
        this.checksum = ChecksumUtil.generateChecksumSHA256(responseType);
    }

    public UDPResponseMessage(String sender, String responseType, Serializable response, String checksum, long sequenceNumber){
        this(sender, responseType, response, checksum);
        this.sequenceNumber = sequenceNumber;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
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
