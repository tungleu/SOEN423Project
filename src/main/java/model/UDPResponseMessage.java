package model;

import util.ChecksumUtil;

import java.io.Serializable;

public class UDPResponseMessage implements Serializable {

    private final String responseType;
    private Serializable response;
    private final String checksum;

    public UDPResponseMessage(String responseType, Serializable response) {
        this(responseType);
        this.response = response;
    }

    public UDPResponseMessage(String responseType) {
        this.responseType = responseType;
        this.checksum = ChecksumUtil.generateChecksumSHA256(responseType);
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
