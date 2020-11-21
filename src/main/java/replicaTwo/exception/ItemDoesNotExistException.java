package replicaTwo.exception;

public class ItemDoesNotExistException extends Exception {
    public ItemDoesNotExistException(String errorMessage) {
        super(errorMessage);
    }
}