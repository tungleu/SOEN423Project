package replicaThree.store;

import common.StoreStrategy;
import replicaThree.client.customerClient;
import replicaThree.common.Province;
import replicaThree.data.ServerData;

import static common.OperationResponse.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Store implements StoreStrategy {


    private Map<String, String> inventory;
    private Map<String, PriorityQueue<String>> waitlist;
    private final Province province;
    private HashMap<String, customerClient> customerClients;
    private HashMap<String, Integer> portMap;
    private ArrayList<String> purchaseLog;
    private ServerData serverData;
    private DateFormat format;

    public Store(ServerData serverData) throws IOException {
        this.serverData = serverData;
        this.province = this.serverData.getProvince();
        this.portMap = serverData.getPortMap();
        this.purchaseLog = serverData.getPurchaseLog();
        this.customerClients = serverData.getCustomerClients();
        this.inventory = serverData.getInventory();
        this.waitlist = serverData.getWaitlist();
        this.format = new SimpleDateFormat("ddMMyyyy");
    }


    public String addItem(String managerID, String itemID, String itemName, int quantity, int price) {
        if (!itemID.substring(0, 2).equals(this.province.toString())) {
            return String.format(ADD_ITEM_ANOTHER_STORE, itemName);
        }
        if (this.inventory.containsKey(itemID)) {
            String[] info = this.inventory.get(itemID).split(",");
            if (price != Integer.parseInt(info[2])) {
                return String.format(ADD_ITEM_INVALID_PRICE, price);
            }
            info[1] = Integer.toString(Integer.parseInt(info[1]) + quantity);
            inventory.replace(itemID, String.join(",", info));

        } else {
            inventory.put(itemID, itemName + "," + quantity + "," + price);


        }
        if (this.waitlist.containsKey(itemID)) {
            PriorityQueue<String> queue = this.waitlist.get(itemID);
            for (String id : queue) {
                Date now = new Date();
                if (id.startsWith(this.province.toString())) {
                    this.purchaseItem(id, itemID, this.format.format(now));
                } else {
                    int port = this.portMap.get(id.substring(0, 2));
                    String message = "PURCHASE_2," + id + "," + itemID + "," + this.format.format(now);
                    this.sendMessage(port, message);
                }
            }
        }
        return String.format(ADD_ITEM_SUCCESS, itemName, itemID, quantity, price);

    }

    public String removeItem(String managerID, String itemID, int quantity) {
        if (!itemID.substring(0, 2).equals(this.province.toString())) {
            return String.format(REMOVE_ITEM_ANOTHER_STORE, itemID);
        }
        if (this.inventory.containsKey(itemID)) {
            String[] info = this.inventory.get(itemID).split(",");
            int current_quantity = Integer.parseInt(info[1]);
            if (quantity == -1) {
                this.inventory.remove(itemID);
                return String.format(REMOVE_ITEM_SUCCESS, itemID, info[0]);
            }
            if (current_quantity < quantity) {
                this.inventory.remove(itemID);
                return String.format(REMOVE_ITEM_BEYOND_QUANTITY, quantity, itemID);
            } else {
                info[1] = Integer.toString(Integer.parseInt(info[1]) - quantity);
                inventory.replace(itemID, String.join(",", info));
                return String.format(REMOVE_ITEM_SUCCESS, info[0], itemID);
            }
        } else {
            System.out.println("The given item ID doesn't exist");
            return String.format(REMOVE_ITEM_NOT_EXISTS, itemID);
        }
    }

    public String listItemAvailability(String managerID) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<String, String> entry : this.inventory.entrySet()) {
            String[] itemInfo = entry.getValue().split(",");
            String name = itemInfo[0];
            int quantity = Integer.parseInt(itemInfo[1]);
            int price = Integer.parseInt(itemInfo[2]);
            sb.append(String.format(LIST_ITEM_AVAILABILITY_SINGLE_SUCCESS, name, entry.getKey(), quantity, price));
            sb.append(",");

        }
        if (sb.charAt(sb.length() - 1) == ',')
            sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    public String purchaseItem(String customerID, String itemID, String dateOfPurchase) {
        String serverName = itemID.substring(0, 2);
        customerClient customer = this.getCustomer(customerID);
        if (serverName.equals(this.province.toString())) {
            return this.purchaseLocalItem(customerID, itemID, dateOfPurchase);
        } else {
            if (customer.checkEligible(serverName)) {
                String message = "PURCHASE" + "," + customerID + "," + customer.getBudget() + "," + itemID + "," + dateOfPurchase;
                String result = this.sendMessage(this.portMap.get(itemID.substring(0, 2)), message);
                if (result.startsWith("SUCCESSFUL")) {
                    String budgetReturn = result.split(",")[1].trim();
                    int returnBudget = Integer.parseInt(budgetReturn);
                    customer.setBudget(returnBudget);
                    customer.setEligibility(serverName, false);
                    return String.format(PURCHASE_ITEM_SUCCESS, itemID);
                } else {
                    return result.trim();
                }
            } else {
                return String.format(PURCHASE_ITEM_ANOTHER_STORE_LIMIT, itemID);
            }
        }
    }

    public String purchaseFromOutside(String customerID, int budget, String itemID, String dateOfPurchase) {
        if (this.inventory.containsKey(itemID)) {
            String[] info = this.inventory.get(itemID).split(",");
            if (Integer.parseInt(info[1]) > 0) {
                if (budget > Integer.parseInt(info[2])) {
                    int returnBudget = budget - Integer.parseInt(info[2]);
                    info[1] = Integer.toString(Integer.parseInt(info[1]) - 1);
                    inventory.replace(itemID, String.join(",", info));
                    this.purchaseLog.add(itemID + "," + customerID + "," + dateOfPurchase);
                    return "SUCCESSFUL," + returnBudget;
                } else {
                    return String.format(PURCHASE_ITEM_NOT_ENOUGH_FUNDS, itemID);
                }
            } else {
                return String.format(PURCHASE_ITEM_OUT_OF_STOCK, itemID);
            }
        } else {
            System.out.println("The given item ID doesn't exist");
            return String.format(PURCHASE_ITEM_OUT_OF_STOCK, itemID);
        }
    }

    public String purchaseLocalItem(String customerID, String itemID, String dateOfPurchase) {
        customerClient customer = this.getCustomer(customerID);
        if (this.inventory.containsKey(itemID)) {
            String[] info = this.inventory.get(itemID).split(",");
            if (Integer.parseInt(info[1]) > 0) {
                if (customer.getBudget() > Integer.parseInt(info[2])) {
                    customer.setBudget(customer.getBudget() - Integer.parseInt(info[2]));
                    info[1] = Integer.toString(Integer.parseInt(info[1]) - 1);
                    inventory.replace(itemID, String.join(",", info));
                    this.purchaseLog.add(itemID + "," + customerID + "," + dateOfPurchase);
                    return String.format(PURCHASE_ITEM_SUCCESS, itemID);
                } else {
                    return String.format(PURCHASE_ITEM_NOT_ENOUGH_FUNDS, itemID);
                }
            } else {
                return String.format(PURCHASE_ITEM_OUT_OF_STOCK, itemID);
            }
        } else {
            System.out.println("The given item ID doesn't exist");
            return String.format(PURCHASE_ITEM_DOES_NOT_EXIST, itemID);
        }
    }

    public synchronized String findItem(String customerID, String itemName) {
        StringBuilder sb = new StringBuilder(itemName).append(": {");
        sb.append(this.findLocalItem(itemName));
        String message = "ITEM_INFO," + itemName;
        for (Map.Entry<String, Integer> entry : this.portMap.entrySet()) {
            if (entry.getKey().equals(this.province.toString())) {
                continue;
            }
            if (sb.toString().equals(""))
                sb.append(",");
            sb.append(this.sendMessage(entry.getValue(), message).trim());
        }
        if (sb.charAt(sb.length() - 1) == ',')
            sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    public String findLocalItem(String itemName) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : this.inventory.entrySet()) {
            String[] itemInfo = entry.getValue().split(",");
            String name = itemInfo[0];
            int quantity = Integer.parseInt(itemInfo[1]);
            int price = Integer.parseInt(itemInfo[2]);
            if (itemName.trim().equals(name)) {
                sb.append(String.format(FIND_ITEM_SINGLE_SUCCESS, entry.getKey(), quantity, price));
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public synchronized String returnItem(String customerID, String itemID, String dateOfReturn) {
        customerClient customer = this.getCustomer(customerID);
        if (itemID.substring(0, 2).equals(this.province.toString())) {
            String eligibility = this.checkReturnElgible(customerID, itemID, dateOfReturn);
            if (eligibility.equals("eligible")) {
                String[] info = this.inventory.get(itemID).split(",");
                this.addItem("Return Manager", itemID, info[0], 1, Integer.parseInt(info[2]));
                customer.setBudget(customer.getBudget() + Integer.parseInt(info[2]));
                return String.format(RETURN_ITEM_SUCCESS, itemID);
            } else if (eligibility.equals("expired")) {
                return String.format(RETURN_ITEM_POLICY_ERROR, itemID);
            } else {
                return String.format(RETURN_ITEM_CUSTOMER_NEVER_PURCHASED, itemID);
            }
        } else {
            int port = this.portMap.get(itemID.substring(0, 2));
            String reply = this.sendMessage(port, "RETURN," + itemID + "," + customerID + "," + dateOfReturn);
            if (reply.startsWith("EXPIRED")) {
                return String.format(RETURN_ITEM_POLICY_ERROR, itemID);
            } else if (reply.startsWith("TRUE")) {
                customer.setBudget(customer.getBudget() + Integer.parseInt(reply.split(",")[1].trim()));
                return String.format(RETURN_ITEM_SUCCESS, itemID);
            } else
                return String.format(RETURN_ITEM_CUSTOMER_NEVER_PURCHASED, itemID);
        }
    }

    public String returnItemfromOutside(String customerID, String itemID, String dateOfReturn) {
        String eligibility = this.checkReturnElgible(customerID, itemID, dateOfReturn);
        if (eligibility.equals("eligible")) {
            String[] info = this.inventory.get(itemID).split(",");
            this.addItem("Return Manager", itemID, info[0], 1, Integer.parseInt(info[2]));
            String price = this.inventory.get(itemID).split(",")[1];
            return "TRUE," + price;
        } else if (eligibility.equals("expired")) {
            return "EXPIRED";
        } else {
            return "NEVER_PURCHASED";
        }
    }

    public String exchangeItem(String customerID, String newitemID, String oldItemID, String dateOfExchange) {
        //check return eligibility
        customerClient customer = this.getCustomer(customerID);
        if (oldItemID.startsWith(this.province.toString())) {
            String eligibility = this.checkReturnElgible(customerID, oldItemID, dateOfExchange);
            if (eligibility.equals("expired")) {

                return String.format(EXCHANGE_ITEM_POLICY_ERROR, oldItemID);
            } else if (eligibility.equals("not purchased")) {
                return String.format(EXCHANGE_ITEM_CUSTOMER_NEVER_PURCHASED, oldItemID);
            }
        } else {
            String eligibility = this.sendMessage(this.portMap.get(oldItemID.substring(0, 2)),
                    "RETURN_ELIGIBLE," + customerID + "," + oldItemID + "," + dateOfExchange).trim();
            if (eligibility.equals("expired")) {
                return String.format(EXCHANGE_ITEM_POLICY_ERROR, oldItemID);
            } else if (eligibility.equals("not purchased")) {
                return String.format(EXCHANGE_ITEM_CUSTOMER_NEVER_PURCHASED, oldItemID);
            }
        }

        //check item in stock
        int oldItemPrice = 0;
        int newItemPrice = 0;
        if (newitemID.startsWith(this.province.toString())) {
            if(!this.inventory.containsKey(newitemID)){
                return String.format(PURCHASE_ITEM_DOES_NOT_EXIST, newitemID);
            }
            String[] itemInfo = this.inventory.get(newitemID).split(",");
            if (Integer.parseInt(itemInfo[1]) == 0) {
                return String.format(EXCHANGE_ITEM_OUT_OF_STOCK, oldItemID, newitemID, newitemID);
            }
            oldItemPrice = Integer.parseInt(itemInfo[2]);
        } else {
            if (!oldItemID.substring(0, 2).equals(newitemID.substring(0, 2)) && !customer.checkEligible(newitemID.substring(0, 2))) {
                return String.format(EXCHANGE_ITEM_ANOTHER_STORE_LIMIT, oldItemID, newitemID);
            }
            String itemAvailability = this.sendMessage(this.portMap.get(newitemID.substring(0, 2)),
                    "ITEM_INFO_2," + newitemID).trim();
            if (itemAvailability.equals("not available")) {
                return String.format(PURCHASE_ITEM_DOES_NOT_EXIST, newitemID);
            } else if (itemAvailability.equals("out of stock")) {
                return String.format(EXCHANGE_ITEM_OUT_OF_STOCK, oldItemID, newitemID, newitemID);
            } else {
                String[] itemInfo = itemAvailability.split(",");
                newItemPrice = Integer.parseInt(itemInfo[2]);
            }

        }
        //check budget
        int requiredBudget = Math.max(0, newItemPrice - oldItemPrice);
        if (customer.getBudget() < requiredBudget) {
            return String.format(EXCHANGE_ITEM_NOT_ENOUGH_FUNDS, oldItemID, newitemID, customerID);
        }
        this.returnItem(customerID, oldItemID, dateOfExchange);
        customer.setEligibility(oldItemID.substring(0, 2), true);
        this.purchaseItem(customerID, newitemID, dateOfExchange);
        return String.format(EXCHANGE_ITEM_SUCCESS, oldItemID, newitemID);
    }

    public String getInfo(String itemID) {
        if (this.inventory.containsKey(itemID)) {
            if (this.inventory.get(itemID).split(",")[1].equals("0"))
                return "out of stock";
            else
                return this.inventory.get(itemID);
        } else {
            return "not available";
        }
    }

    public String checkReturnElgible(String customerID, String itemID, String date) {
        try {
            for (String log : this.purchaseLog) {
                String[] logParams = log.split(",");
                if (logParams[0].equals(itemID) && logParams[1].equals(customerID)) {
                    Date datePurchased = this.format.parse(logParams[2]);
                    long day30 = 30l * 24 * 60 * 60 * 1000;
                    if (this.format.parse(date).compareTo(new Date(datePurchased.getTime() + day30)) < 0) {
                        return "eligible";
                    } else
                        return "expired";
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "not purchased";
    }

    public synchronized String addWaitList(String customerID, String itemID) {
        if (itemID.substring(0, 2).equals(this.province.toString())) {
            return this.addLocalWaitList(customerID, itemID);
        } else {
            int port = this.portMap.get(itemID.substring(0, 2));
            String message = "WAITLIST," + customerID + "," + itemID;
            return this.sendMessage(port, message).trim();
        }
    }

    public String addLocalWaitList(String customerID, String itemID) {
        if (this.waitlist.containsKey(itemID)) {
            this.waitlist.get(itemID).add(customerID);
        } else {
            PriorityQueue<String> queue = new PriorityQueue<String>();
            queue.add(customerID);
            this.waitlist.put(itemID, queue);
        }
        return String.format(ADD_WAIT_LIST, customerID);
    }

    public String sendMessage(int serverPort, String messageToSend) {
        DatagramSocket aSocket = null;
        String replyMessage = null;
        try {
            aSocket = new DatagramSocket();
            byte[] message = messageToSend.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message, messageToSend.length(), aHost, serverPort);
            aSocket.send(request);
            System.out.println("Request message sent from the client to server with port number " + serverPort + " is: "
                    + new String(request.getData()));
            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            aSocket.receive(reply);
            replyMessage = new String(reply.getData());
            System.out.println("Reply received from the server with port number " + serverPort + " is: "
                    + new String(reply.getData()));
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();

        }
        return replyMessage;
    }

    public DatagramSocket startUDPServer(AtomicBoolean isRunning) {
        try {
            DatagramSocket aSocket = new DatagramSocket(this.portMap.get(this.province.toString()));
            System.out.println("UDP Server for " + this.province.toString() + " has started listening............");
            new Thread(() -> {
                String replyMessage = null;
                byte[] buffer = new byte[1000];
                while (isRunning.get()) {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    try {
                        aSocket.receive(request);
                        String[] requestArgs = new String(request.getData()).split(",");
                        switch (requestArgs[0]) {
                            case "PURCHASE": {
                                String customerID = requestArgs[1];
                                int budget = Integer.parseInt(requestArgs[2]);
                                String itemID = requestArgs[3];
                                String date = requestArgs[4];
                                replyMessage = this.purchaseFromOutside(customerID, budget, itemID, date);
                                break;
                            }
                            case "WAITLIST": {
                                String customerID = requestArgs[1];
                                String itemID = requestArgs[2];
                                this.addLocalWaitList(customerID, itemID);
                                break;
                            }
                            case "ITEM_INFO":
                                String itemName = requestArgs[1];
                                replyMessage = this.findLocalItem(itemName);
                                break;
                            case "ITEM_INFO_2":
                                replyMessage = this.getInfo(requestArgs[1].trim());
                                break;
                            case "RETURN": {
                                String itemID = requestArgs[1];
                                String customerID = requestArgs[2];
                                String dateOfReturn = requestArgs[3];
                                replyMessage = this.returnItemfromOutside(customerID, itemID, dateOfReturn);
                                break;
                            }
                            case "PURCHASE_2": {
                                String customerID = requestArgs[1];
                                String itemID = requestArgs[2];
                                String date = requestArgs[3];
                                replyMessage = this.purchaseItem(customerID, itemID, date);
                                break;
                            }
                            case "RETURN_ELIGIBLE": {
                                String customerID = requestArgs[1];
                                String itemID = requestArgs[2];
                                String date = requestArgs[3];
                                replyMessage = String.valueOf(this.checkReturnElgible(customerID, itemID, date));
                                break;
                            }
                        }
                        assert replyMessage != null;
                        DatagramPacket reply = new DatagramPacket(replyMessage.getBytes(), replyMessage.length(), request.getAddress(),
                                request.getPort());
                        aSocket.send(reply);
                        buffer = new byte[1000];
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            return aSocket;
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        }
        return null;
    }

    public customerClient getCustomer(String customerID) {
        if (!customerClients.containsKey(customerID)) {
            customerClient customer = new customerClient(customerID);
            this.customerClients.put(customer.getID(), customer);
        }
        return this.customerClients.get(customerID);
    }
}
