package frontend;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static common.ReplicaConstants.*;
import static util.StringUtil.sortStr;


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

    public long getInitialTime() {
        return timeCreated;
    }

    public String getFinalResponse() {
        Map<String, List<String>> logContainer = new HashMap<>();
        reverseResponses.forEach((k, v) -> logContainer.computeIfAbsent(responses.get(v.get(0)), logKey -> v));
        logger.info("Final response for sequence number " + sequenceNumber + ": " + logContainer);

        String validReplicaName = reverseResponses.values().stream().max(Comparator.comparing(List::size)).get().get(0);
        return responses.get(validReplicaName);
    }

    public void addResponse(String replicaManager, String response) {
        this.responses.put(replicaManager, response);
        String sortedResponse = sortStr(response);
        this.reverseResponses.computeIfAbsent(sortedResponse, k -> new ArrayList<>());
        this.reverseResponses.get(sortedResponse).add(replicaManager);
    }

    public boolean isEqual() {
        return reverseResponses.size() <= 1;
    }

    public synchronized long getSequenceNumber() {
        return sequenceNumber;
    }

    public synchronized void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getFailureReplicaManager() {
        if (responses.size() != 3) {
            for (String name : REPLICA_MANAGER_NAMES) {
                if (!responses.containsKey(name)) {
                    logger.severe("Missing response from: " + name);
                    return name;
                }
            }
        }
        String brokenReplicaName = reverseResponses.values().stream().min(Comparator.comparing(List::size)).get().get(0);
        logger.severe("Invalid response from: " + brokenReplicaName);
        return brokenReplicaName;
    }
}
