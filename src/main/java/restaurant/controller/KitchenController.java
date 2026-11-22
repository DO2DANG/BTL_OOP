package restaurant.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import restaurant.exception.InvalidOrderStateException;
import restaurant.exception.ResourceNotFoundException;
import restaurant.model.OrderStatus;
import restaurant.model.User;
import restaurant.service.OrderService;
import restaurant.view.KitchenView;

public final class KitchenController implements WebController {
    private final OrderService orderService;
    private final User operator;

    public KitchenController(OrderService orderService, User operator) {
        this.orderService = Objects.requireNonNull(orderService, "OrderService không được để trống.");
        this.operator = Objects.requireNonNull(operator, "Người trực bếp không được để trống.");
        if (!operator.canManageKitchen()) {
            throw new IllegalArgumentException("Tài khoản '" + operator.getName() + "' không có quyền điều phối bếp.");
        }
    }

    @Override
    public void register(HttpServer server) throws IOException {
        HttpSupport.register(server, "/", "GET", this::showOrders);
        HttpSupport.register(server, "/status", "POST", this::changeStatus);
        HttpSupport.registerFavicon(server);
        HttpSupport.registerAsset(server, "/assets/kitchen.css", "text/css; charset=UTF-8");
        HttpSupport.registerAsset(server, "/assets/kitchen.js", "text/javascript; charset=UTF-8");
    }

    private void showOrders(HttpExchange exchange) throws IOException {
        String page = KitchenView.buildOwnerHtml(
                orderService.getOrders(),
                operator,
                orderService.calculateTotalRevenue());
        HttpSupport.html(exchange, 200, page);
    }

    private void changeStatus(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> fields = HttpSupport.readForm(exchange);
            int orderId = HttpSupport.readNumber(fields, "orderId", "Mã đơn không hợp lệ.");
            OrderStatus status = readStatus(fields.getOrDefault("status", ""));
            orderService.changeStatus(orderId, status);
            HttpSupport.redirect(exchange, "/");
        } catch (ResourceNotFoundException exception) {
            HttpSupport.text(exchange, 404, exception.getMessage());
        } catch (InvalidOrderStateException exception) {
            HttpSupport.text(exchange, 409, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            HttpSupport.text(exchange, 400, exception.getMessage());
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
