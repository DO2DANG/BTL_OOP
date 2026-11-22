package restaurant;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import restaurant.controller.CustomerController;
import restaurant.controller.KitchenController;
import restaurant.controller.WebController;
import restaurant.model.RestaurantOwner;
import restaurant.model.User;
import restaurant.repository.FileOrderRepository;
import restaurant.repository.InMemoryMenuRepository;
import restaurant.repository.InMemoryOrderRepository;
import restaurant.repository.MenuRepository;
import restaurant.repository.OrderRepository;
import restaurant.service.OrderService;
import restaurant.service.RestaurantService;

public final class RestaurantSystem {
    private static final int CUSTOMER_PORT = 8080;
    private static final int KITCHEN_PORT = 8081;

    private RestaurantSystem() {
    }

    public static void main(String[] args) throws IOException {
        boolean useFileStorage = Arrays.asList(args).contains("--file");

        MenuRepository menuRepository = new InMemoryMenuRepository();
        OrderRepository orderRepository = useFileStorage
                ? new FileOrderRepository(Path.of("data", "orders.txt"), menuRepository)
                : new InMemoryOrderRepository();
        OrderService orderService = new RestaurantService(menuRepository, orderRepository);
        User owner = new RestaurantOwner("chef_admin", "Bếp Trưởng Gà Vàng");

        WebController customerController = new CustomerController(orderService);
        WebController kitchenController = new KitchenController(orderService, owner);

        int workerThreads = Math.max(4, Runtime.getRuntime().availableProcessors());
        ExecutorService httpExecutor = Executors.newFixedThreadPool(workerThreads);

        HttpServer customerServer = openServer(CUSTOMER_PORT);
        HttpServer kitchenServer = null;
        try {
            kitchenServer = openServer(KITCHEN_PORT);
            customerServer.setExecutor(httpExecutor);
            kitchenServer.setExecutor(httpExecutor);
            customerController.register(customerServer);
            kitchenController.register(kitchenServer);
            customerServer.start();
            kitchenServer.start();
        } catch (IOException | RuntimeException exception) {
            customerServer.stop(0);
            if (kitchenServer != null) {
                kitchenServer.stop(0);
            }
            httpExecutor.shutdownNow();
            throw exception;
        }

        HttpServer activeKitchenServer = kitchenServer;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            customerServer.stop(0);
            activeKitchenServer.stop(0);
            httpExecutor.shutdown();
        }, "restaurant-shutdown"));

        System.out.println("Chế độ lưu đơn: " + (useFileStorage ? "Tệp (data/orders.txt)" : "RAM (InMemory)"));
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
