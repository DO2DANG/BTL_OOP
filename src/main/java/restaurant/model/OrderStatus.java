package restaurant.model;

public enum OrderStatus {
    PENDING("Chờ chuẩn bị"),
    COOKING("Bếp đang chế biến"),
    READY("Đã hoàn tất"),
    CANCELLED("Đã hủy");

    private final String title;

    OrderStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public boolean canMoveTo(OrderStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> next == COOKING || next == CANCELLED;
            case COOKING -> next == READY || next == CANCELLED;
            case READY, CANCELLED -> false;
        };
    }
}
