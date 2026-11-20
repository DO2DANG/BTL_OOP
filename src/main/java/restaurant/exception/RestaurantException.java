package restaurant.exception;

public abstract class RestaurantException extends Exception {
    private static final long serialVersionUID = 1L;

    protected RestaurantException(String message) {
        super(message);
    }

    protected RestaurantException(String message, Throwable cause) {
        super(message, cause);
    }
}
