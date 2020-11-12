package util;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import replica.ReplicaOne;
import replica.common.Replica;
import replica.data.ReplicaOneData;
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

    public static Address fetchAddressForDataTransfer(String rmName, String replicaName, JChannel channel) {
        return channel.view().getMembers().stream().filter(address -> {
            String name = address.toString();
            return name.contains(replicaName) && !name.contains(rmName);
        }).findFirst().get();
    }

    public static Replica startReplica(String replicaManager, String replica, @Nullable Message message) throws Exception {
        // TODO(#14): Add other code bases for replica and their corresponding data fetch
        switch (replica) {
            case REPLICA_TWO:
            case REPLICA_THREE:
            default:
                return new ReplicaOne(replicaManager + replica, fetchReplicaOneData(message)).start();
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

}
