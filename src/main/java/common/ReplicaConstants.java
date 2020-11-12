package common;

public final class ReplicaConstants {

    private ReplicaConstants() {
    }

    public static final String SEQUENCER_REPLICA_CLUSTER = "SequencerReplicaCluster";
    public static final String CLIENT_RM_CLUSTER = "ClientReplicaManagerCluster";
    public static final String REPLACE_RM_CLUSTER = "ReplaceRMCluster";

    public static final String QC_SERVER_NAME = "QC";
    public static final String BC_SERVER_NAME = "BC";
    public static final String ON_SERVER_NAME = "ON";

    public static final String REPLICA_MANAGER_ONE = "ReplicaManagerOne";
    public static final String REPLICA_MANAGER_TWO = "ReplicaManagerTwo";
    public static final String REPLICA_MANAGER_THREE = "ReplicaManagerThree";
    public static final String[] REPLICA_MANAGER_NAMES = {REPLICA_MANAGER_ONE, REPLICA_MANAGER_TWO, REPLICA_MANAGER_THREE};

    public static final String REPLICA_ONE = "ReplicaOne";
    public static final String REPLICA_TWO = "ReplicaTwo";
    public static final String REPLICA_THREE = "ReplicaThree";
    public static final String[] REPLICA_NAMES = {REPLICA_ONE, REPLICA_TWO, REPLICA_THREE};
}
