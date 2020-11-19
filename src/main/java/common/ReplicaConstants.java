package common;

public final class ReplicaConstants {

    private ReplicaConstants() {
    }

    public static final String FRONT_END_SERVER_NAME = "FrontEndServer";
    public static final String SEQUENCER_REPLICA_CLUSTER = "SequencerReplicaCluster";
    public static final String CLIENT_RM_CLUSTER = "ClientReplicaManagerCluster";
    public static final String REPLICA_RM_CLUSTER = "ReplicaRMCluster";
    public static final String CLIENT_REPLICA_CLUSTER = "ClientReplicaCluster";
    public static final String CLIENT_SEQUENCER_CLUSTER = "ClientSequencerCluster";

    public static final String QC_SERVER_NAME = "QC";
    public static final String BC_SERVER_NAME = "BC";
    public static final String ON_SERVER_NAME = "ON";
    public static final String[] SERVER_NAMES = {QC_SERVER_NAME, BC_SERVER_NAME, ON_SERVER_NAME};

    public static final String REPLICA_MANAGER_ONE = "ReplicaManagerOne";
    public static final String REPLICA_MANAGER_TWO = "ReplicaManagerTwo";
    public static final String REPLICA_MANAGER_THREE = "ReplicaManagerThree";
    public static final String[] REPLICA_MANAGER_NAMES = {REPLICA_MANAGER_ONE, REPLICA_MANAGER_TWO, REPLICA_MANAGER_THREE};

    public static final String REPLICA_ONE = "ReplicaOne";
    public static final String REPLICA_TWO = "ReplicaTwo";
    public static final String REPLICA_THREE = "ReplicaThree";
    public static final String[] REPLICA_NAMES = {REPLICA_ONE, REPLICA_TWO, REPLICA_THREE};

    public static final String SEQUENCER_NAME = "Sequencer";


    public static final String LOG_REPLICA_START = "Replica %s is online";
    public static final String LOG_REPLICA_KILL = "Replica %s was killed";
    public static final String LOG_REPLICA_REQUEST_RECEIVED = "Replica %s received request: {%s}";
    public static final String LOG_REPLICA_REQUEST_RESULT = "Replica %s result for sequence number %d: {%s}";
    public static final String LOG_REPLICA_DATA_TRANSFER_REQUEST = "Replica %s received data transfer request";

    public static final String REPLICA_NAME_DELIMETER = "_";

}
