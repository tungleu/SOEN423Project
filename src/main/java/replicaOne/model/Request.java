package replicaOne.model;

import replicaOne.server.requests.RequestType;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Kevin Tan 2020-09-21
 */
public class Request implements Serializable {

    private final RequestType requestType;
    private final List<String> params;

    public Request(RequestType requestType, List<String> params) {
        this.requestType = requestType;
        this.params = params;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public List<String> getParams() {
        return params;
    }
}
