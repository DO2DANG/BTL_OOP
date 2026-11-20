package restaurant.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import restaurant.model.Order;

public final class InMemoryOrderRepository implements OrderRepository {
    private static final int INITIAL_ORDER_ID = 501;

    private final List<Order> orders = new ArrayList<>();
    private int sequence = INITIAL_ORDER_ID;

    @Override
    public synchronized int nextOrderId() {
        return sequence++;
    }

    @Override
    public synchronized void save(Order order) {
        Objects.requireNonNull(order, "Đơn hàng lưu trữ không được để trống.");
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).getOrderId() == order.getOrderId()) {
                orders.set(i, order);
                return;
            }
        }
        orders.add(0, order);
        if (order.getOrderId() >= sequence) {
            sequence = order.getOrderId() + 1;
        }
    }

    @Override
    public synchronized Optional<Order> findById(int orderId) {
        return orders.stream().filter(order -> order.getOrderId() == orderId).findFirst();
    }

    @Override
    public synchronized List<Order> findAll() {
        return List.copyOf(orders);
    }
}
