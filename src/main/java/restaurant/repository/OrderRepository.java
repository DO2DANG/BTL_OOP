package restaurant.repository;

import java.util.List;
import java.util.Optional;
import restaurant.model.Order;

public interface OrderRepository {
    int nextOrderId();

    void save(Order order);

    Optional<Order> findById(int orderId);

    List<Order> findAll();
}
