package replicaTwo.store;

import replica.data.ReplicaTwoData;
import replicaTwo.data.inventory.Item;
import replicaTwo.data.inventory.StoreInventory;
import replicaTwo.data.sales.SalesManager;
import replicaTwo.exception.*;
import replicaTwo.udp.data.DataHandlerUDP;
import replicaTwo.udp.request.RequestDispatcher;
import replicaTwo.udp.request.RequestDispatcherUDP;
import replicaTwo.udp.request.RequestHandler;
import replicaTwo.udp.request.RequestHandlerUDP;
import replicaTwo.util.StoreUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static replicaTwo.udp.request.RequestTypesUDP.*;

public class Store {
    public class StoreServerUDP implements Runnable {
        private final int port;
        private final RequestHandler requestHandler;
        private final ExecutorService executor;

        public StoreServerUDP(int port) {
            this.port = port;
            this.requestHandler = new RequestHandlerUDP(Store.this.inventory, Store.this.salesManager);
            this.executor = Executors.newWorkStealingPool();
        }

        public void run() {
            try (DatagramSocket aSocket = new DatagramSocket(port)) {
                System.out.println("Store started on port: " + port);
                while (true) {
                    byte[] buffer = new byte[20000];
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(request);
                    List<String> args = DataHandlerUDP.unmarshall(request);

                    this.executor.execute(() -> {
                        List<String> executionResult = Collections.singletonList(UDP_STATUS_FAILURE);
                        try {
                            switch (args.get(0)) {
                                case FIND_ITEM:
                                    String itemName = args.get(1);
                                    executionResult = this.requestHandler.findItem(itemName);
                                    break;
                                case PURCHASE_ITEM: {
                                    int budget = Integer.parseInt(args.get(1));
                                    String itemID = args.get(2);
                                    executionResult = this.requestHandler.purchaseItem(itemID, budget);
                                    break;
                                }
                                case RETURN_ITEM: {
                                    String itemID = args.get(1);
                                    String itemPrice = String.valueOf(this.requestHandler.getItemPrice(itemID));
                                    Store.this.addItem(itemID);
                                    executionResult = Collections.singletonList(itemPrice);
                                    break;
                                }
                                case ADD_CUSTOMER_TO_WAIT_QUEUE: {
                                    String customerID = args.get(1);
                                    String itemID = args.get(2);
                                    this.requestHandler.appendToWaitQueue(customerID, itemID);
                                    executionResult = Collections.singletonList(UDP_STATUS_SUCCESS);
                                    break;
                                }
                                case AUTOMATICALLY_ASSIGN_ITEM: {
                                    String customerID = args.get(1);
                                    String itemID = args.get(2);
                                    try {
                                        Store.this.purchaseItem(customerID, itemID, StoreUtils.getCurrentDateString());
                                    } catch(Exception ignore){}
                                    executionResult = Collections.singletonList(UDP_STATUS_SUCCESS);
                                    break;
                                }
                                case FETCH_PRODUCT_PRICE: {
                                    String itemID = args.get(1);
                                    int itemPrice = this.requestHandler.getItemPrice(itemID);
                                    executionResult = Collections.singletonList(String.valueOf(itemPrice));
                                    break;
                                }
                            }

                            byte[] buf = DataHandlerUDP.marshall(executionResult);
                            DatagramPacket reply = new DatagramPacket(buf, buf.length, request.getAddress(), request.getPort());
                            aSocket.send(reply);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.executor.shutdown();
            }
        }
    }

    private final String locationName;
    private final StoreInventory inventory;
    private final SalesManager salesManager;
    private final RequestDispatcher requestDispatcher;

    public Store(String locationName, ReplicaTwoData replicaTwoData, Map<String, Integer> portsConfig) {
        super();
        this.locationName = locationName;
        this.inventory = replicaTwoData.getInventoryOnLocation(this.locationName);
        this.salesManager = replicaTwoData.getSalesManagerOnLocation(this.locationName);
        this.requestDispatcher = new RequestDispatcherUDP(this.locationName, portsConfig);
    }

    public Item addItem(String managerID, String itemID, String itemName, int quantity, int price) throws ManagerItemPriceMismatchException {
        if(this.inventory.isItemPriceMismatch(itemID, price)) {
            throw new ManagerItemPriceMismatchException("Add item failed. item price does not match");
        }

        this.inventory.addItemToStock(itemID, itemName, quantity, price);
        this.automaticallyAssignItem(itemID);
        return this.inventory.getItem(itemID);
    }

    public String removeItem(String managerID, String itemID, int quantity) throws ManagerRemoveBeyondQuantityException, ManagerRemoveNonExistingItemException {
        if(!this.inventory.isItemInStock(itemID)) {
            throw new ManagerRemoveNonExistingItemException("item does not exist");
        }

        if(!this.inventory.isEnoughItemQuantity(itemID, quantity)) {
            throw new ManagerRemoveBeyondQuantityException("Can not remove beyond the quantity.");
        }
        String itemName = this.inventory.getItemName(itemID);

        if(quantity < 0) {
            this.inventory.removeItemFromStock(itemID);
        } else {
            this.inventory.reduceItemQuantityInStock(itemID, quantity);
        }
        return itemName;
    }

    public String listItemAvailability(String managerID) {
        return this.inventory.getStock();
    }

    public String purchaseItem(String customerID, String itemID, String dateOfPurchase) throws ItemOutOfStockException,
            NotEnoughFundsException, ExternalStorePurchaseLimitException {
        String targetStore = StoreUtils.getStoreFromDescriptor(itemID);
        long purchaseTimestamp = StoreUtils.parseDate(dateOfPurchase);

        if(this.salesManager.isRemotePurchaseLimit(customerID, targetStore)) {
            throw new ExternalStorePurchaseLimitException("External purchase limit!");
        }

        if(this.currentStore(targetStore)) {
            this.performLocalPurchase(customerID, itemID, purchaseTimestamp);
        } else {
            int itemPrice = this.verifyRemoteStorePurchase(targetStore, customerID, itemID);
            this.salesManager.checkoutRemotePurchase(customerID, itemID, itemPrice, purchaseTimestamp, targetStore);
        }
        return "SUCCESS";
    }

    public String findItem(String customerID, String itemName) {
        List<String> collectedItems = new ArrayList<>(this.inventory.getStockByName(itemName));
        collectedItems.addAll(this.requestDispatcher.broadcastCollect(Arrays.asList(FIND_ITEM, itemName)));
        return String.join(", ", collectedItems);
    }

    public String returnItem(String customerID, String itemID, String dateOfReturn) throws CustomerNeverPurchasedItemException, ReturnPolicyException {
        String targetStore = StoreUtils.getStoreFromDescriptor(itemID);
        long returnTimestamp = StoreUtils.parseDate(dateOfReturn);
        long returnWindow = StoreUtils.generateReturnWindow(dateOfReturn);

        Long purchaseTimestamp = this.verifyItemReturn(customerID, itemID, returnWindow, returnTimestamp);
        this.performItemReturn(targetStore, customerID, itemID, purchaseTimestamp);

        return "SUCCESS";
    }

    public String exchangeItem(String customerID, String newItemID, String oldItemID, String dateOfExchange) throws ReturnPolicyException,
            CustomerNeverPurchasedItemException, ExternalStorePurchaseLimitException, ItemOutOfStockException, NotEnoughFundsException
    {
        String newItemTargetStore = StoreUtils.getStoreFromDescriptor(newItemID);
        String oldItemTargetStore = StoreUtils.getStoreFromDescriptor(oldItemID);
        long exchangeTimestamp = StoreUtils.parseDate(dateOfExchange);
        long returnWindow = StoreUtils.generateReturnWindow(dateOfExchange);

        long oldItemPurchaseTimestamp = this.verifyItemReturn(customerID, oldItemID, returnWindow, exchangeTimestamp);

        if(oldItemID.equals(newItemID)) {
            this.salesManager.replacePurchaseTimestamp(customerID, oldItemID, oldItemPurchaseTimestamp, exchangeTimestamp);
            return "SUCCESS";
        }

        int oldItemPrice = 0;
        if(this.currentStore(oldItemTargetStore)) {
            oldItemPrice = this.inventory.getItemPrice(oldItemID);
        } else {
            oldItemPrice = Integer.parseInt(
                    this.requestDispatcher.unicast(Arrays.asList(FETCH_PRODUCT_PRICE, oldItemID), oldItemTargetStore).get(0));
        }

        this.salesManager.increaseCustomerBudget(customerID, oldItemPrice);
        try {
            if(!this.currentStore(newItemTargetStore) && oldItemTargetStore.equals(newItemTargetStore)) {
                int newItemPrice = this.verifyRemoteStorePurchase(newItemTargetStore, customerID, newItemID);
                this.performItemReturn(oldItemTargetStore, customerID, oldItemID, oldItemPurchaseTimestamp);
                this.salesManager.checkoutRemotePurchase(customerID, newItemID, newItemPrice, exchangeTimestamp, newItemTargetStore);
            } else {
                this.purchaseItem(customerID, newItemID, dateOfExchange);
                this.performItemReturn(oldItemTargetStore, customerID, oldItemID, oldItemPurchaseTimestamp);
            }
        } finally {
            this.salesManager.decreaseCustomerBudget(customerID, oldItemPrice);
        }
        return "SUCCESS";
    }

    public String addCustomerToWaitQueue(String customerID, String itemID) {
        String targetStore = StoreUtils.getStoreFromDescriptor(itemID);
        if(this.currentStore(targetStore)) {
            this.salesManager.appendCustomerToWaitQueue(customerID, itemID);
            this.automaticallyAssignItem(itemID);
        } else {
            this.requestDispatcher.unicast(Arrays.asList(ADD_CUSTOMER_TO_WAIT_QUEUE, customerID, itemID), targetStore);
        }
        return "SUCCESS";
    }

    public void listen(int port) {
        new Thread(new StoreServerUDP(port)).start();
    }

    private void performItemReturn(String targetStore, String customerID, String itemID, long purchaseTimestamp) {
        int itemPrice = 0;
        if(this.currentStore(targetStore)) {
            this.addItem(itemID);
            itemPrice = this.inventory.getItemPrice(itemID);
        } else {
            itemPrice = Integer.parseInt(this.requestDispatcher.unicast(Arrays.asList(RETURN_ITEM, itemID), targetStore).get(0));
        }
        this.salesManager.refundCustomer(targetStore, customerID, itemID, itemPrice, purchaseTimestamp);
    }

    private Long verifyItemReturn(String customerID, String itemID, long returnWindow, long returnTimestamp) throws CustomerNeverPurchasedItemException, ReturnPolicyException {
        if(!this.salesManager.isCustomerPurchasedItem(customerID, itemID)) {
            throw new CustomerNeverPurchasedItemException("item with ID: " + itemID + " was never purchased " +
                    "by customer with ID: " + customerID);
        }

        Long purchaseTimestamp = this.salesManager.getValidPurchaseTimestamp(customerID, itemID, returnWindow, returnTimestamp);
        if(purchaseTimestamp == null) {
            throw new ReturnPolicyException("Purchase was made beyond the return policy.");
        }
        return purchaseTimestamp;
    }

    private int verifyLocalStorePurchase(String customerID, String itemID) throws ItemOutOfStockException, NotEnoughFundsException {
        if(!this.inventory.isItemInStockWithQuantity((itemID))) {
            throw new ItemOutOfStockException("item with ID: " + itemID + " is out of stock.");
        }

        int itemPrice = this.inventory.getItemPrice(itemID);
        if(!this.salesManager.isCustomerWithEnoughFunds(customerID, itemPrice)) {
            throw new NotEnoughFundsException("Customer does not have enough funds!");
        }
        return itemPrice;
    }

    private int verifyRemoteStorePurchase(String remoteStore, String customerID, String itemID) throws ItemOutOfStockException, NotEnoughFundsException {
        String purchaseResult = this.requestDispatcher.unicast(
                Arrays.asList(PURCHASE_ITEM, String.valueOf(this.salesManager.getCustomerBudget(customerID)), itemID),
                remoteStore).get(0);

        if(purchaseResult.equals(ItemOutOfStockException.class.getSimpleName())) {
            throw new ItemOutOfStockException("item with ID: " + itemID + " is out of stock.");
        }
        if(purchaseResult.equals(NotEnoughFundsException.class.getSimpleName())) {
            throw new NotEnoughFundsException("Customer does not have enough funds!");
        }

        return Integer.parseInt(purchaseResult);
    }

    private void addItem(String itemID) {
        if(this.inventory.isItemInStock(itemID)) {
            this.inventory.addItemToStock(itemID, this.inventory.getItemName(itemID), 1, this.inventory.getItemPrice(itemID));
            this.automaticallyAssignItem(itemID);
        }
    }

    private void performLocalPurchase(String customerID, String itemID, long purchaseTimestamp) throws NotEnoughFundsException, ItemOutOfStockException {
        int itemPrice = this.verifyLocalStorePurchase(customerID, itemID);
        this.salesManager.checkoutLocalPurchase(customerID, itemID, itemPrice, purchaseTimestamp);
        this.inventory.reduceItemQuantityInStock(itemID);
    }

    private void automaticallyAssignItem(String itemID) {
        while(this.inventory.isItemInStockWithQuantity(itemID) && this.salesManager.isItemWithWaitList(itemID)) {
            String customerID = this.salesManager.fetchWaitListedCustomer(itemID);
            if(customerID == null) { continue; }
            String targetStore = StoreUtils.getStoreFromDescriptor(customerID);

            try {
                if(this.currentStore(targetStore)) {
                    this.performLocalPurchase(customerID, itemID, System.currentTimeMillis());
                } else {
                    this.requestDispatcher.unicast(Arrays.asList(AUTOMATICALLY_ASSIGN_ITEM, customerID, itemID), targetStore);
                }
            } catch (Exception ignored) {}
        }
    }

    private boolean currentStore(String targetStore) {
        return this.locationName.equals(targetStore);
    }
}
