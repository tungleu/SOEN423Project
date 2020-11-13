package replicamanager;

import static common.ReplicaConstants.REPLICA_MANAGER_NAMES;
import static common.ReplicaConstants.REPLICA_NAMES;

public class ReplicaManagerServer {

    public static void main(String[] args) {
        String replicaOption = "", replicaManagerName = "";
        for (int i = 0; i < args.length; i++) {
            if ("-replica".equals(args[i])) {
                replicaOption = REPLICA_NAMES[Integer.parseInt(args[++i])];
            } else if ("-replicaManager".equals(args[i])) {
                replicaManagerName = REPLICA_MANAGER_NAMES[Integer.parseInt(args[++i])];
            } else {
                help();
            }
        }

        try {
            ReplicaManager replicaManager = new ReplicaManager(replicaManagerName);
            // Cold start Replica & RM
            replicaManager.coldStart(replicaOption);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void help() {
        System.out.println("ReplicaManager [-replica replica] [-replicaManager replicaManager]");
        System.exit(1);
    }
}
