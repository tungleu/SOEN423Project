package common;

public interface StoreStrategy {
    String addItem(String managerID, String itemID, String itemName, int quantity, int price);
    String removeItem(String managerID, String itemID, int quantity);
    String listItemAvailability(String managerID);
    String purchaseItem(String customerID, String itemID, String dateOfPurchase);
    String findItem(String customerID, String itemName);
    String returnItem(String customerID, String itemID, String dateOfReturn);
    String exchangeItem(String customerID, String newItemID, String oldItemID, String dateOfExchange);
    String addWaitList(String customerID, String itemID);
}
