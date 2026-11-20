package restaurant.service;

import java.util.List;
import restaurant.exception.EmptyCartException;
import restaurant.exception.InvalidOrderStateException;
import restaurant.exception.ResourceNotFoundException;
import restaurant.model.CartSession;
import restaurant.model.Customer;
import restaurant.model.MenuItem;
import restaurant.model.Order;
import restaurant.model.OrderStatus;

public interface OrderService {
    List<MenuItem> getMenu();

    MenuItem getMenuItem(int productId) throws ResourceNotFoundException;

    List<Order> getOrders();

    Order submitOrder(Customer customer, CartSession cart) throws EmptyCartException;

    void changeStatus(int orderId, OrderStatus status)
            throws ResourceNotFoundException, InvalidOrderStateException;

    long calculateTotalRevenue();
}
