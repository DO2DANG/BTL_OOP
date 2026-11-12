package restaurant;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import restaurant.controller.CustomerController;
import restaurant.controller.KitchenController;
import restaurant.model.RestaurantOwner;
import restaurant.service.RestaurantService;

public final class RestaurantSystem {
    private static final int CUSTOMER_PORT = 8080;
    private static final int KITCHEN_PORT = 8081;

    private RestaurantSystem() {
    }

    public static void main(String[] args) throws IOException {
        RestaurantService restaurant = new RestaurantService();
        RestaurantOwner owner = new RestaurantOwner("chef_admin", "Bếp Trưởng Gà Vàng");

        HttpServer customerServer = HttpServer.create(new InetSocketAddress(CUSTOMER_PORT), 0);
        HttpServer kitchenServer = null;
        try {
            kitchenServer = HttpServer.create(new InetSocketAddress(KITCHEN_PORT), 0);
            new CustomerController(restaurant).register(customerServer);
            new KitchenController(restaurant, owner).register(kitchenServer);
            customerServer.start();
            kitchenServer.start();
        } catch (IOException | RuntimeException exception) {
            customerServer.stop(0);
            if (kitchenServer != null) {
                kitchenServer.stop(0);
            }
            throw exception;
        }

        System.out.println("Khách hàng: http://localhost:" + CUSTOMER_PORT);
        System.out.println("Màn hình bếp: http://localhost:" + KITCHEN_PORT);
    }
}
