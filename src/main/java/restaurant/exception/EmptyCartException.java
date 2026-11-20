package restaurant.exception;

public final class EmptyCartException extends RestaurantException {
    private static final long serialVersionUID = 1L;

    public EmptyCartException() {
        super("Giỏ hàng đang trống. Vui lòng chọn ít nhất một món trước khi đặt đơn.");
    }
}
