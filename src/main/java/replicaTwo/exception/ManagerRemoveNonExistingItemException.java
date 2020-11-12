package replicaTwo.exception;

public class ManagerRemoveNonExistingItemException extends Exception{
    public ManagerRemoveNonExistingItemException(String errorMessage) {
        super(errorMessage);
    }
}
