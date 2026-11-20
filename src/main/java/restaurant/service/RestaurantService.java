package restaurant.service;

import java.util.List;
import java.util.Objects;
import restaurant.exception.EmptyCartException;
import restaurant.exception.InvalidOrderStateException;
import restaurant.exception.ResourceNotFoundException;
import restaurant.model.CartSession;
import restaurant.model.Customer;
import restaurant.model.MenuItem;
import restaurant.model.Order;
import restaurant.model.OrderItem;
import restaurant.model.OrderStatus;
import restaurant.repository.InMemoryMenuRepository;
import restaurant.repository.InMemoryOrderRepository;
import restaurant.repository.MenuRepository;
import restaurant.repository.OrderRepository;

public final class RestaurantService implements OrderService {
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;

    public RestaurantService() {
        this(new InMemoryMenuRepository(), new InMemoryOrderRepository());
    }

    public RestaurantService(MenuRepository menuRepository, OrderRepository orderRepository) {
        this.menuRepository = Objects.requireNonNull(menuRepository, "MenuRepository không được để trống.");
        this.orderRepository = Objects.requireNonNull(orderRepository, "OrderRepository không được để trống.");
    }

    @Override
    public List<MenuItem> getMenu() {
        return menuRepository.findAll();
    }

    @Override
    public MenuItem getMenuItem(int productId) throws ResourceNotFoundException {
        return menuRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Món ăn #" + productId + " không có trong thực đơn."));
    }

    @Override
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order submitOrder(Customer customer, CartSession cart) throws EmptyCartException {
        Objects.requireNonNull(customer, "Khách đặt đơn không được để trống.");
        Objects.requireNonNull(cart, "Giỏ hàng không được để trống.");
        List<OrderItem> items = cart.drainItems();
        synchronized (this) {
            int orderId = orderRepository.nextOrderId();
            Order order = new Order(orderId, customer, items);
            orderRepository.save(order);
            return order;
        }
    }

    @Override
    public synchronized void changeStatus(int orderId, OrderStatus status)
            throws ResourceNotFoundException, InvalidOrderStateException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId + "."));
        order.changeStatus(status);
        orderRepository.save(order);
    }

    @Override
    public long calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.READY)
                .mapToLong(Order::calculateTotal)
                .sum();
    }
}
