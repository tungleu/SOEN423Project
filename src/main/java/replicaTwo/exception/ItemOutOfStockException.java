package replicaTwo.exception;

public class ItemOutOfStockException extends Exception {
    public ItemOutOfStockException(String errorMessage) {
        super(errorMessage);
    }
}