package vn.edu.ptit.restaurant.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.edu.ptit.restaurant.exception.InvalidPhoneNumberException;
import vn.edu.ptit.restaurant.model.CartSession;
import vn.edu.ptit.restaurant.model.Customer;
import vn.edu.ptit.restaurant.model.MenuItem;
import vn.edu.ptit.restaurant.model.Order;
import vn.edu.ptit.restaurant.service.RestaurantService;
import vn.edu.ptit.restaurant.view.CustomerView;

public final class CustomerController {
    private static final String CART_COOKIE = "restaurantCart=";

    private final RestaurantService restaurant;
    private final Map<String, CartSession> carts = new ConcurrentHashMap<>();

    public CustomerController(RestaurantService restaurant) {
        this.restaurant = restaurant;
    }

    public void register(HttpServer server) throws IOException {
        HttpSupport.register(server, "/", "GET", this::showMenu);
        HttpSupport.register(server, "/cart/add", "POST", this::addProduct);
        HttpSupport.register(server, "/cart/quantity", "POST", this::changeQuantity);
        HttpSupport.register(server, "/cart/submit", "POST", this::submitOrder);
        HttpSupport.registerAsset(server, "/assets/customer.css", "text/css; charset=UTF-8");
        HttpSupport.registerAsset(server, "/assets/customer.js", "text/javascript; charset=UTF-8");
    }

    private void showMenu(HttpExchange exchange) throws IOException {
        CartSession cart = getCart(exchange);
        HttpSupport.html(exchange, 200, CustomerView.buildCustomerHtml(restaurant.getMenu(), cart));
    }

    private void addProduct(HttpExchange exchange) throws IOException {
        CartSession cart = getCart(exchange);
        try {
            Map<String, String> fields = HttpSupport.readForm(exchange);
            int productId = HttpSupport.readNumber(fields, "id", "Mã món không hợp lệ.");
            MenuItem product = restaurant.findMenuItem(productId).orElse(null);
            if (product == null) {
                sendCart(exchange, cart, 404, "Món ăn không có trong thực đơn.");
                return;
            }
            cart.addProduct(product, fields.get("note"));
            sendCart(exchange, cart, 200, "");
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
            Order order = restaurant.submitOrder(customer, cart);
            sendCart(exchange, cart, 200, "Đặt món thành công! Đơn #" + order.getOrderId() + " đã được gửi tới bếp.");
        } catch (InvalidPhoneNumberException | IllegalArgumentException exception) {
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
        HttpSupport.html(exchange, status, CustomerView.buildCartInnerHtml(cart, notification));
    }
}
