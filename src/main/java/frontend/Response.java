package frontend;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Response {
    private final Date timeCreated;
    private final Map<String, String> responses;
    private long sequenceNumber;

    public Response() {
        this.timeCreated = new Date();
        this.responses = new ConcurrentHashMap<>();

    }
    public Collection<String> getResponses() {
        return responses.values();
    }

    public Map<String, String> getResponseMap(){
        return responses;
    }

    public Date getInitialTime(){
        return timeCreated;
    }

    public String getFinalResponse(){
        Set<String> values = new HashSet<>();
        for(String value : this.getResponses()){
            if(!values.add(value))
                return value;
        }
        return "";
    }

    public void addResponse(String replicaManager, String response){
        this.responses.put(replicaManager, response);
    }

    public boolean isEqual(){
        return this.getResponses().stream().distinct().count() <= 1;

    }

    public synchronized long getSequenceNumber() {
        return sequenceNumber;
    }

    public synchronized void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
