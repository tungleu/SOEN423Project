package replicaTwo.exception;

public class ManagerRemoveBeyondQuantityException extends Exception{
    public ManagerRemoveBeyondQuantityException(String errorMessage) {
        super(errorMessage);
    }
}
