package restaurant.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Map;
import restaurant.model.OrderStatus;
import restaurant.model.RestaurantOwner;
import restaurant.service.RestaurantService;
import restaurant.view.KitchenView;

public final class KitchenController {
    private final RestaurantService restaurant;
    private final RestaurantOwner owner;

    public KitchenController(RestaurantService restaurant, RestaurantOwner owner) {
        this.restaurant = restaurant;
        this.owner = owner;
    }

    public void register(HttpServer server) throws IOException {
        HttpSupport.register(server, "/", "GET", this::showOrders);
        HttpSupport.register(server, "/status", "POST", this::changeStatus);
        HttpSupport.registerAsset(server, "/assets/kitchen.css", "text/css; charset=UTF-8");
        HttpSupport.registerAsset(server, "/assets/kitchen.js", "text/javascript; charset=UTF-8");
    }

    private void showOrders(HttpExchange exchange) throws IOException {
        String page = KitchenView.buildOwnerHtml(restaurant.getOrders(), owner, restaurant.calculateTotalRevenue());
        HttpSupport.html(exchange, 200, page);
    }

    private void changeStatus(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> fields = HttpSupport.readForm(exchange);
            int orderId = HttpSupport.readNumber(fields, "orderId", "Mã đơn không hợp lệ.");
            OrderStatus status = readStatus(fields.getOrDefault("status", ""));
            if (!restaurant.changeStatus(orderId, status)) {
                HttpSupport.text(exchange, 404, "Không tìm thấy đơn hàng.");
                return;
            }
            HttpSupport.redirect(exchange, "/");
        } catch (IllegalArgumentException exception) {
            HttpSupport.text(exchange, 400, exception.getMessage());
        } catch (IllegalStateException exception) {
            HttpSupport.text(exchange, 409, exception.getMessage());
        }
    }

    private OrderStatus readStatus(String value) {
        try {
            return OrderStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Trạng thái đơn không hợp lệ.", exception);
        }
    }
}
