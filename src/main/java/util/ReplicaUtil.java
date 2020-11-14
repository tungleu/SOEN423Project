package util;

import org.jgroups.Message;
import replica.ReplicaOne;
import replica.ReplicaTwo;
import replica.common.Replica;
import replica.data.ReplicaOneData;
import replica.data.ReplicaTwoData;
import replicaOne.model.ServerInventory;

import javax.annotation.Nullable;

import static common.ReplicaConstants.*;

public final class ReplicaUtil {

    private ReplicaUtil() {
    }

    public static String chooseReplica(String name) {
        for (String replica : REPLICA_MANAGER_NAMES) {
            if (!name.equals(replica)) return replica;
        }
        // Should never happen
        return null;
    }

    public static Replica startReplica(String replicaManager, String replica, @Nullable Message message) throws Exception {
        // TODO(#14): Add other code bases for replica and their corresponding data fetch
        String name = replicaManager + replica;
        switch (replica) {
            case REPLICA_TWO:
                return new ReplicaTwo(name, fetchReplicaTwoData(message)).start();
            case REPLICA_THREE:
            default:
                return new ReplicaOne(name, fetchReplicaOneData(message)).start();
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

}
