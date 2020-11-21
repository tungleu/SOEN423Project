package frontend;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static common.ReplicaConstants.*;
import static common.ReplicaConstants.REPLICA_MANAGER_ONE;


public class Response {
    private final long timeCreated;
    private final Map<String, String> responses;
    private long sequenceNumber;
    private final Logger logger;
    private final Map<String, List<String>> reverseResponses;

    public Response(Logger logger) {
        this.timeCreated = System.currentTimeMillis();
        this.responses = new ConcurrentHashMap<>();
        this.logger = logger;
        this.reverseResponses = new ConcurrentHashMap<>();
    }

    public Collection<String> getResponses() {
        return responses.values();
    }

    public Map<String, String> getResponseMap() {
        return responses;
    }

    public long getInitialTime() {
        return timeCreated;
    }

    public String getFinalResponse() {
        logger.info("Final response for sequence number " + sequenceNumber + ": " + reverseResponses);
        Set<String> values = new HashSet<>();
        for (String value : this.getResponses()) {
            if (!values.add(value))
                return value;
        }
        return "";
    }

    public void addResponse(String replicaManager, String response) {
        this.responses.put(replicaManager, response);
        this.reverseResponses.computeIfAbsent(response, k -> new ArrayList<>());
        this.reverseResponses.get(response).add(replicaManager);
    }

    public boolean isEqual() {
        return this.getResponses().stream().distinct().count() <= 1;
    }

    public synchronized long getSequenceNumber() {
        return sequenceNumber;
    }

    public synchronized void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getFailureReplicaManager() {
        String replicaManager = "";
        if (responses.values().size() != 3) {
            for (String name : REPLICA_MANAGER_NAMES) {
                if (!responses.containsKey(name)) {
                    return name;
                }
            }
        } else {
            String responseOne = this.responses.get(REPLICA_MANAGER_ONE);
            if (responseOne.equals(this.responses.get(REPLICA_MANAGER_TWO))) {
                replicaManager = REPLICA_MANAGER_THREE;
            } else if (responseOne.equals(this.responses.get(REPLICA_MANAGER_THREE))) {
                replicaManager = REPLICA_MANAGER_TWO;
            } else {
                replicaManager = REPLICA_MANAGER_ONE;
            }
            return replicaManager;
        }
        return replicaManager;
    }
}
