package restaurant.exception;

public final class InvalidOrderStateException extends RestaurantException {
    private static final long serialVersionUID = 1L;

    public InvalidOrderStateException(int orderId, String currentTitle, String targetTitle) {
        super("Đơn #" + orderId + " đang ở trạng thái '" + currentTitle
                + "' nên không thể chuyển sang '" + targetTitle + "'.");
    }
}
