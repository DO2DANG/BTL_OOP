package restaurant.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import restaurant.exception.InvalidOrderStateException;

public final class Order {
    private final int orderId;
    private final Customer customer;
    private final List<OrderItem> items;
    private final LocalDateTime createdAt;
    private volatile OrderStatus status;

    public Order(int orderId, Customer customer, List<OrderItem> items) {
        this(orderId, customer, items, LocalDateTime.now(), OrderStatus.PENDING);
    }

    public Order(
            int orderId,
            Customer customer,
            List<OrderItem> items,
            LocalDateTime createdAt,
            OrderStatus status) {
        this.orderId = Validation.requirePositive(orderId, "Mã đơn hàng");
        this.customer = Objects.requireNonNull(customer, "Thông tin khách hàng không được để trống.");
        this.items = List.copyOf(Objects.requireNonNull(items, "Danh sách món không được để trống."));
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất một món.");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "Thời điểm tạo đơn không được để trống.");
        this.status = Objects.requireNonNull(status, "Trạng thái đơn không được để trống.");
    }

    public int getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public synchronized void changeStatus(OrderStatus next) throws InvalidOrderStateException {
        Objects.requireNonNull(next, "Trạng thái đích không được để trống.");
        if (status == next) {
            return;
        }
        if (!status.canMoveTo(next)) {
            throw new InvalidOrderStateException(orderId, status.getTitle(), next.getTitle());
        }
        status = next;
    }

    public long calculateTotal() {
        return items.stream().mapToLong(OrderItem::getSubtotal).sum();
    }

    public int estimatePrepMinutes() {
        int maxItemPrep = items.stream()
                .mapToInt(orderItem -> orderItem.getItem().getEstimatedPrepMinutes())
                .max()
                .orElse(0);
        int extraItems = Math.max(0, items.stream().mapToInt(OrderItem::getQuantity).sum() - 1);
        return maxItemPrep + Math.min(10, extraItems * 2);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Order that)) {
            return false;
        }
        return orderId == that.orderId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(orderId);
    }

    @Override
    public String toString() {
        return "Order#" + orderId + "[" + status + ", total=" + calculateTotal() + " đ]";
    }
}
