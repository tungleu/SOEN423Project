package replica.data;

import replicaThree.common.Province;
import replicaThree.data.ServerData;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class ReplicaThreeData implements Serializable {

    private final ServerData BCData;
    private final ServerData ONData;
    private final ServerData QCData;

    public ReplicaThreeData() {
        this.BCData = new ServerData(Province.BC);
        this.ONData = new ServerData(Province.ON);
        this.QCData = new ServerData(Province.QC);
    }

    public ServerData getBCData() {
        return BCData;
    }

    public ServerData getONData() {
        return ONData;
    }

    public ServerData getQCData() {
        return QCData;
    }

    public void resetPorts(){
        BCData.resetPorts();
        ONData.resetPorts();
        QCData.resetPorts();
    }
}
