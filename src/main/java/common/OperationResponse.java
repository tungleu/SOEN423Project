package common;

public final class OperationResponse {
    private OperationResponse(){}
    public static final String ADD_ITEM_SUCCESS = "Inventory updated with item name: %s, item id: %s, item quantity: %d, item price: %.2f";
    public static final String ADD_ITEM_INVALID_PRICE = "Updating item with invalid price: %.2f";
    public static final String ADD_ITEM_ANOTHER_STORE = "Cannot update item %s, does not belong in store";
    public static final String REMOVE_ITEM_SUCCESS = "Inventory updated with item name: %s, item id: %s";
    public static final String REMOVE_ITEM_NOT_EXISTS = "Item %s does not exist";
    public static final String REMOVE_ITEM_BEYOND_QUANTITY = "Cannot remove %d of item %s since it has %d quantity";
    public static final String REMOVE_ITEM_ANOTHER_STORE = "Cannot update item %s, doesn’t belong in store";
    public static final String PURCHASE_ITEM_SUCCESS = "Purchase item %s successful";
    public static final String PURCHASE_ITEM_OUT_OF_STOCK = "Cannot purchase item %s, do you want to be added to the waitlist?";
    public static final String PURCHASE_ITEM_NOT_ENOUGH_FUNDS = "Cannot purchase item %s, not enough funds";
    public static final String PURCHASE_ITEM_ANOTHER_STORE_LIMIT = "Cannot purchase item %s, limited to max 1 per external customer";
    public static final String RETURN_ITEM_SUCCESS = "Item %s successfully returned";
    public static final String RETURN_ITEM_POLICY_ERROR = "Item %s cannot be returned, passed return policy";
    public static final String RETURN_ITEM_CUSTOMER_NEVER_PURCHASED = "Item %s cannot be returned, item never purchased";
    public static final String EXCHANGE_ITEM_SUCCESS = "Exchange item %s for item %s successful";
    public static final String EXCHANGE_ITEM_POLICY_ERROR = "Could not exchange item %s, passed return policy";
    public static final String EXCHANGE_ITEM_CUSTOMER_NEVER_PURCHASED = "Could not exchange item %s, item never purchased";
    public static final String FIND_ITEM_SINGLE_SUCCESS = "(itemId:%s, quantity:%d, price:%.2f)";
    public static final String LIST_ITEM_AVAILABILITY_SINGLE_SUCCESS = "(itemName:%s, itemId:%s, quantity:%d, price:%.2f)";
    public static final String ADD_WAIT_LIST = "Successfully added customer with Id %s to the waitlist";
}
