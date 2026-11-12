package vn.edu.ptit.restaurant.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public final class Order {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM");

    private final int orderId;
    private final Customer customer;
    private final List<OrderItem> items;
    private final LocalDateTime createdAt;
    private volatile OrderStatus status = OrderStatus.PENDING;

    public Order(int orderId, Customer customer, List<OrderItem> items) {
        this.orderId = orderId;
        this.customer = Objects.requireNonNull(customer);
        this.items = List.copyOf(items);
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng đang trống.");
        }
        this.createdAt = LocalDateTime.now();
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

    public String getCreatedAt() {
        return createdAt.format(TIME_FORMAT);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public synchronized void changeStatus(OrderStatus next) {
        if (status == next) {
            return;
        }
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Đơn #" + orderId + " không thể chuyển sang trạng thái này.");
        }
        status = next;
    }

    public long calculateTotal() {
        return items.stream().mapToLong(OrderItem::getSubtotal).sum();
    }
}
