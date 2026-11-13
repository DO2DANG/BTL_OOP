package restaurant;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.BindException;
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

        HttpServer customerServer = openServer(CUSTOMER_PORT);
        HttpServer kitchenServer = null;
        try {
            kitchenServer = openServer(KITCHEN_PORT);
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

        System.out.println("Khách hàng: http://localhost:" + customerServer.getAddress().getPort());
        System.out.println("Màn hình bếp: http://localhost:" + kitchenServer.getAddress().getPort());
    }

    private static HttpServer openServer(int preferredPort) throws IOException {
        try {
            return HttpServer.create(new InetSocketAddress(preferredPort), 0);
        } catch (BindException portInUse) {
            return HttpServer.create(new InetSocketAddress(0), 0);
        }
    }
}
