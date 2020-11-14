package replica.common;

import common.StoreStrategy;
import model.UDPRequestMessage;

import java.util.List;

public class RequestExecutor {
    public String execute(StoreStrategy storeStrategy, UDPRequestMessage operationRequest) {
        List<String> params = operationRequest.getParameters();
        switch(operationRequest.getRequestType()) {
            case ADD_ITEM:
                return storeStrategy.addItem(params.get(0), params.get(1), params.get(2), Integer.parseInt(params.get(3)), Integer.parseInt(params.get(4)));
            case REMOVE_ITEM:
                return storeStrategy.removeItem(params.get(0), params.get(1), Integer.parseInt(params.get(2)));
            case LIST_ITEM_AVAILABILITY:
                return storeStrategy.listItemAvailability(params.get(0));
            case PURCHASE_ITEM:
                return storeStrategy.purchaseItem(params.get(0), params.get(1), params.get(2));
            case FIND_ITEM:
                return storeStrategy.findItem(params.get(0), params.get(1));
            case RETURN_ITEM:
                return storeStrategy.returnItem(params.get(0), params.get(1), params.get(2));
            case EXCHANGE_ITEM:
                return storeStrategy.exchangeItem(params.get(0), params.get(1), params.get(2), params.get(3));
            case ADD_WAIT_LIST:
                return storeStrategy.addWaitList(params.get(0), params.get(1));
            default:
                return "Unknown operation request";
        }
    }
}
