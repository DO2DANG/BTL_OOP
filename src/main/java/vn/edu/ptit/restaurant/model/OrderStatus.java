package vn.edu.ptit.restaurant.model;

public enum OrderStatus {
    PENDING("Chờ chuẩn bị", "#d97706"),
    COOKING("Bếp đang chế biến", "#0284c7"),
    READY("Đã hoàn tất", "#16a34a"),
    CANCELLED("Đã hủy", "#dc2626");

    private final String title;
    private final String color;

    OrderStatus(String title, String color) {
        this.title = title;
        this.color = color;
    }

    public String getTitle() {
        return title;
    }

    public String getColor() {
        return color;
    }

    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == COOKING || next == CANCELLED;
            case COOKING -> next == READY;
            case READY, CANCELLED -> false;
        };
    }
}
