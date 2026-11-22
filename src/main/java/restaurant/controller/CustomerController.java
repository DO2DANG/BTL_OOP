package restaurant.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import restaurant.exception.ResourceNotFoundException;
import restaurant.exception.RestaurantException;
import restaurant.model.CartSession;
import restaurant.model.Customer;
import restaurant.model.MenuItem;
import restaurant.model.Order;
import restaurant.service.OrderService;
import restaurant.view.CustomerView;

public final class CustomerController implements WebController {
    private static final String CART_COOKIE = "restaurantCart=";
    private static final int MAX_ACTIVE_CARTS = 1000;

    private final OrderService orderService;
    private final Map<String, CartSession> carts = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CartSession> eldest) {
                    return size() > MAX_ACTIVE_CARTS;
                }
            });

    public CustomerController(OrderService orderService) {
        this.orderService = Objects.requireNonNull(orderService, "OrderService không được để trống.");
    }

    @Override
    public void register(HttpServer server) throws IOException {
        HttpSupport.register(server, "/", "GET", this::showMenu);
        HttpSupport.register(server, "/cart/add", "POST", this::addProduct);
        HttpSupport.register(server, "/cart/quantity", "POST", this::changeQuantity);
        HttpSupport.register(server, "/cart/submit", "POST", this::submitOrder);
        HttpSupport.registerFavicon(server);
        HttpSupport.registerAsset(server, "/assets/customer.css", "text/css; charset=UTF-8");
        HttpSupport.registerAsset(server, "/assets/customer.js", "text/javascript; charset=UTF-8");
    }

    private void showMenu(HttpExchange exchange) throws IOException {
        CartSession cart = getCart(exchange);
        HttpSupport.html(exchange, 200, CustomerView.buildCustomerHtml(orderService.getMenu(), cart));
    }

    private void addProduct(HttpExchange exchange) throws IOException {
        CartSession cart = getCart(exchange);
        try {
            Map<String, String> fields = HttpSupport.readForm(exchange);
            int productId = HttpSupport.readNumber(fields, "id", "Mã món không hợp lệ.");
            MenuItem product = orderService.getMenuItem(productId);
            cart.addProduct(product, fields.get("note"));
            sendCart(exchange, cart, 200, "");
        } catch (ResourceNotFoundException exception) {
            sendCart(exchange, cart, 404, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            sendCart(exchange, cart, 400, exception.getMessage());
        }
    }

    private void changeQuantity(HttpExchange exchange) throws IOException {
        CartSession cart = getCart(exchange);
        try {
            Map<String, String> fields = HttpSupport.readForm(exchange);
            int productId = HttpSupport.readNumber(fields, "id", "Mã món không hợp lệ.");
            int delta = HttpSupport.readNumber(fields, "delta", "Mức thay đổi số lượng không hợp lệ.");
            if (!cart.changeQty(productId, delta)) {
                sendCart(exchange, cart, 404, "Món ăn không có trong giỏ.");
                return;
            }
            sendCart(exchange, cart, 200, "");
        } catch (IllegalArgumentException exception) {
            sendCart(exchange, cart, 400, exception.getMessage());
        }
    }

    private void submitOrder(HttpExchange exchange) throws IOException {
        CartSession cart = getCart(exchange);
        try {
            Map<String, String> fields = HttpSupport.readForm(exchange);
            Customer customer = new Customer(fields.get("name"), fields.get("table"), fields.get("phone"));
            Order order = orderService.submitOrder(customer, cart);
            sendCart(exchange, cart, 200, "Đặt món thành công! Đơn #" + order.getOrderId() + " đã được gửi tới bếp.");
        } catch (RestaurantException | IllegalArgumentException exception) {
            sendCart(exchange, cart, 400, exception.getMessage());
        }
    }

    private CartSession getCart(HttpExchange exchange) {
        List<String> headers = exchange.getRequestHeaders().get("Cookie");
        if (headers != null) {
            for (String header : headers) {
                for (String part : header.split(";")) {
                    String cookie = part.strip();
                    if (cookie.startsWith(CART_COOKIE)) {
                        CartSession cart = carts.get(cookie.substring(CART_COOKIE.length()));
                        if (cart != null) {
                            return cart;
                        }
                    }
                }
            }
        }

        String cartId = UUID.randomUUID().toString();
        CartSession cart = new CartSession();
        carts.put(cartId, cart);
        exchange.getResponseHeaders().add("Set-Cookie", CART_COOKIE + cartId + "; Path=/; HttpOnly; SameSite=Lax");
        return cart;
    }

    private void sendCart(HttpExchange exchange, CartSession cart, int status, String notification) throws IOException {
        boolean isError = status >= 400;
        HttpSupport.html(exchange, status, CustomerView.buildCartInnerHtml(cart, notification, isError));
    }
}
