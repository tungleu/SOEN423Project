package util;

import org.jgroups.Message;
import replica.ReplicaOne;
import replica.ReplicaThree;
import replica.ReplicaTwo;
import replica.common.Replica;
import replica.data.ReplicaOneData;
import replica.data.ReplicaThreeData;
import replica.data.ReplicaTwoData;
import replicaOne.model.ServerInventory;

import javax.annotation.Nullable;

import static common.ReplicaConstants.*;

public final class ReplicaUtil {

    private ReplicaUtil() {
    }

    public static String chooseReplica(String name) {
        for (String replica : REPLICA_NAMES) {
            if (!name.equals(replica)) return replica;
        }
        // Should never happen
        return null;
    }

    public static Replica startReplica(String replicaManager, String replica, @Nullable Message message) throws Exception {
        String name = replicaManager + REPLICA_NAME_DELIMETER + replica;
        switch (replica) {
            case REPLICA_TWO:
                return new ReplicaTwo(name, fetchReplicaTwoData(message)).start();
            case REPLICA_THREE:
                return new ReplicaThree(name, fetchReplicaThreeData(message)).start();
            default:
                return new ReplicaOne(name, fetchReplicaOneData(message), message == null).start();
        }
    }

    private static ReplicaOneData fetchReplicaOneData(@Nullable Message message) {
        if (message == null) {
            return new ReplicaOneData(new ServerInventory(BC_SERVER_NAME), new ServerInventory(QC_SERVER_NAME),
                                      new ServerInventory(ON_SERVER_NAME));
        } else {
            return message.getObject();
        }
    }

    private static ReplicaTwoData fetchReplicaTwoData(@Nullable Message message) {
        if (message == null) {
            return new ReplicaTwoData();
        }
        return new ReplicaTwoData(message.getObject());
    }

    private static ReplicaThreeData fetchReplicaThreeData(@Nullable Message message) {
        if (message == null) {
            return new ReplicaThreeData();
        } else {
            return message.getObject();
        }
    }

    public static String fetchRMNamefromReplica(String replicaName) {
        return replicaName.split(REPLICA_NAME_DELIMETER)[0];
    }

}
