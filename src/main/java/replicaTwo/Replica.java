package replicaTwo;

import replicaTwo.data.ReplicaData;
import replicaTwo.store.StoreProxy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Replica {
    public Replica(ReplicaData replicaData) {
        Map<String, Integer> portsConfig = new HashMap();
        portsConfig.put("QC", 8887);
        portsConfig.put("ON", 8888);
        portsConfig.put("BC", 8889);

        StoreProxy storeProxyBC = new StoreProxy("BC", replicaData, portsConfig);
        StoreProxy storeProxyQC = new StoreProxy("QC", replicaData, portsConfig);
        StoreProxy storeProxyON = new StoreProxy("BC", replicaData, portsConfig);

        try {
            storeProxyBC.initializeStore(portsConfig.get("BC"));
            storeProxyQC.initializeStore(portsConfig.get("QC"));
            storeProxyON.initializeStore(portsConfig.get("ON"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
