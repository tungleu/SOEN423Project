package replicaTwo.exception;

public class ExternalStorePurchaseLimitException extends Exception {
    public ExternalStorePurchaseLimitException(String errorMessage) {
        super(errorMessage);
    }
}
