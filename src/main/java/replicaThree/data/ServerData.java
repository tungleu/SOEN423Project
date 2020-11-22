package replicaThree.data;

import replicaThree.client.customerClient;
import replicaThree.common.Province;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class ServerData implements Serializable {
    private final Map<String, String> inventory;
    private final Map<String, PriorityQueue<String>> waitlist;
    private HashMap<String, customerClient> customerClients = new HashMap<String, customerClient>();
    private HashMap<String, Integer> portMap = new HashMap<String, Integer>();
    private ArrayList<String> purchaseLog = new ArrayList<String>();
    private final Province province;

    public ServerData(Province province){
        this.province = province;
        this.inventory = new HashMap<String, String>();
        this.waitlist = new HashMap<String, PriorityQueue<String>>();
        this.customerClients = new HashMap<String, customerClient>();
        this.portMap = new HashMap<String, Integer>();
        this.purchaseLog = new ArrayList<String>();
        this.portMap.put("QC", 3331);
        this.portMap.put("ON", 3332);
        this.portMap.put("BC", 3333);
    }

    public Province getProvince() {
        return province;
    }

    public Map<String, String> getInventory() {
        return inventory;
    }

    public Map<String, PriorityQueue<String>> getWaitlist() {
        return waitlist;
    }

    public HashMap<String, customerClient> getCustomerClients() {
        return customerClients;
    }

    public HashMap<String, Integer> getPortMap() {
        return portMap;
    }

    public ArrayList<String> getPurchaseLog() {
        return purchaseLog;
    }

    public void resetPorts(){
        for (Map.Entry<String, Integer> entry : this.portMap.entrySet()){
            int newPort = entry.getValue() + this.portMap.values().size();
            this.portMap.replace(entry.getKey(), newPort);
        }

    }
}
