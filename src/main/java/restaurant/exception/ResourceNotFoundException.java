package restaurant.exception;

public final class ResourceNotFoundException extends RestaurantException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
