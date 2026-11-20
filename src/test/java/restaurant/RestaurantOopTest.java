package restaurant;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import restaurant.controller.CustomerController;
import restaurant.controller.KitchenController;
import restaurant.exception.EmptyCartException;
import restaurant.exception.InvalidOrderStateException;
import restaurant.exception.InvalidPhoneNumberException;
import restaurant.exception.ResourceNotFoundException;
import restaurant.model.Beverage;
import restaurant.model.CartSession;
import restaurant.model.ChickenDish;
import restaurant.model.Customer;
import restaurant.model.MenuCategory;
import restaurant.model.MenuItem;
import restaurant.model.Order;
import restaurant.model.OrderStatus;
import restaurant.model.RestaurantOwner;
import restaurant.model.SideDish;
import restaurant.model.User;
import restaurant.repository.FileOrderRepository;
import restaurant.repository.InMemoryMenuRepository;
import restaurant.repository.InMemoryOrderRepository;
import restaurant.repository.MenuRepository;
import restaurant.repository.OrderRepository;
import restaurant.service.OrderService;
import restaurant.service.RestaurantService;
import restaurant.view.CustomerView;
import restaurant.view.KitchenView;

public final class RestaurantOopTest {
    private static int passed = 0;

    private RestaurantOopTest() {
    }

    public static void main(String[] args) throws Exception {
        testUserInheritanceAndValidation();
        testMenuItemPolymorphismAndInvariants();
        testCartSessionOperations();
        testOrderWorkflowAndStateExceptions();
        testFileOrderRepositoryPersistence();
        testViewsAndPermissionGuard();
        testEndToEndHttpServers();

        System.out.println("HOÀN TẤT KIỂM THỬ OOP: " + passed + " kịch bản đều đạt.");
    }

    private static void testUserInheritanceAndValidation() throws Exception {
        User customer = new Customer("Minh Quân", "Bàn 06", "0912 345 678");
        User owner = new RestaurantOwner("chef_admin", "Bếp Trưởng Gà Vàng");

        check(!customer.canManageKitchen(), "Customer không được có quyền quản lý bếp");
        check(owner.canManageKitchen(), "RestaurantOwner phải có quyền quản lý bếp");
        check("Khách hàng".equals(customer.getRoleName()), "Sai role của Customer");
        check(customer.getProfileSummary().contains("0912345678"), "Chuẩn hóa SĐT chưa đúng");

        Customer guestWithoutPhone = new Customer("Lan Anh", "Mang về", "   ");
        check(guestWithoutPhone.getPhone().isEmpty(), "SĐT bỏ trống phải lưu chuỗi rỗng");

        expectThrows(InvalidPhoneNumberException.class,
                () -> new Customer("Khách lỗi", "Bàn 01", "12345"));
        expectThrows(IllegalArgumentException.class,
                () -> new RestaurantOwner("  ", "Bếp Trưởng"));
        passed++;
    }

    private static void testMenuItemPolymorphismAndInvariants() {
        MenuRepository menuRepo = new InMemoryMenuRepository();
        List<MenuItem> menu = menuRepo.findAll();
        check(menu.size() == 16, "Thực đơn chuẩn phải có 16 món");

        long chickenCount = menu.stream().filter(item -> item instanceof ChickenDish).count();
        long sideCount = menu.stream().filter(item -> item instanceof SideDish).count();
        long drinkCount = menu.stream().filter(item -> item instanceof Beverage).count();
        check(chickenCount == 10 && sideCount == 3 && drinkCount == 3, "Phân loại lớp con MenuItem chưa khớp");

        for (MenuItem item : menu) {
            check(item.getEstimatedPrepMinutes() > 0, "Thời gian chế biến đa hình phải > 0");
            check(!item.getServingSpec().isBlank(), "Đặc tả phục vụ đa hình không được rỗng");
        }

        expectThrows(IllegalArgumentException.class, () -> new ChickenDish(
                0, "Món lỗi", 45000, MenuCategory.COM_GA, "https://example.com/a.jpg", "Mô tả", "Xối mỡ", 10));
        expectThrows(IllegalArgumentException.class, () -> new Beverage(
                99, "Trà lỗi", -5000, "https://example.com/b.jpg", "Mô tả", true, 3));
        passed++;
    }

    private static void testCartSessionOperations() throws Exception {
        MenuRepository menuRepo = new InMemoryMenuRepository();
        MenuItem comGa = menuRepo.findById(1).orElseThrow();
        MenuItem traDao = menuRepo.findById(14).orElseThrow();

        CartSession cart = new CartSession();
        expectThrows(EmptyCartException.class, cart::drainItems);

        cart.addProduct(comGa, "Thêm sốt mắm");
        cart.addProduct(comGa, "");
        cart.addProduct(traDao, "Ít đá");

        CartSession.Snapshot snap = cart.snapshot();
        check(snap.totalQuantity() == 3, "Tổng số lượng phải là 3");
        check(snap.totalPrice() == comGa.getPrice() * 2 + traDao.getPrice(), "Tổng tiền giỏ hàng sai");
        check("Thêm sốt mắm".equals(snap.items().get(0).getNote()), "Ghi chú cũ phải được giữ khi thêm lần 2 không ghi chú");

        cart.changeQty(comGa.getId(), -1);
        check(cart.getTotalQuantity() == 2, "Giảm số lượng chưa đúng");
        cart.changeQty(comGa.getId(), -1);
        check(cart.getItems().size() == 1, "Số lượng về 0 phải tự xóa món khỏi giỏ");
        passed++;
    }

    private static void testOrderWorkflowAndStateExceptions() throws Exception {
        OrderService service = new RestaurantService(new InMemoryMenuRepository(), new InMemoryOrderRepository());
        CartSession cart = new CartSession();
        cart.addProduct(service.getMenuItem(1), "Da giòn");
        cart.addProduct(service.getMenuItem(15), "");

        Customer customer = new Customer("Hoàng Nam", "Bàn 03", "+84 987-654-321");
        Order created = service.submitOrder(customer, cart);

        check(cart.snapshot().isEmpty(), "Đặt đơn xong giỏ hàng phải được làm trống");
        check(created.getStatus() == OrderStatus.PENDING, "Đơn mới phải ở trạng thái PENDING");
        check(service.calculateTotalRevenue() == 0L, "Đơn chưa READY chưa được cộng doanh thu");

        service.changeStatus(created.getOrderId(), OrderStatus.COOKING);
        service.changeStatus(created.getOrderId(), OrderStatus.READY);
        check(service.calculateTotalRevenue() == 65000L, "Doanh thu đơn READY tính chưa đúng");

        expectThrows(InvalidOrderStateException.class,
                () -> service.changeStatus(created.getOrderId(), OrderStatus.CANCELLED));
        expectThrows(ResourceNotFoundException.class,
                () -> service.changeStatus(9999, OrderStatus.COOKING));
        passed++;
    }

    private static void testFileOrderRepositoryPersistence() throws Exception {
        Path tempFile = Files.createTempFile("btl-oop-orders-", ".txt");
        try {
            MenuRepository menuRepo = new InMemoryMenuRepository();
            OrderRepository fileRepo1 = new FileOrderRepository(tempFile, menuRepo);
            OrderService service1 = new RestaurantService(menuRepo, fileRepo1);

            CartSession cart = new CartSession();
            cart.addProduct(service1.getMenuItem(4), "Không cay | nhiều hành");
            Customer customer = new Customer("Thu Hà", "Mang về", "0901234567");
            Order saved = service1.submitOrder(customer, cart);
            service1.changeStatus(saved.getOrderId(), OrderStatus.COOKING);

            // Khởi tạo lại repository từ cùng tệp để kiểm chứng lưu trữ bền vững
            OrderRepository fileRepo2 = new FileOrderRepository(tempFile, menuRepo);
            List<Order> reloaded = fileRepo2.findAll();
            check(reloaded.size() == 1, "Đọc lại đơn hàng từ tệp thất bại");
            Order order = reloaded.get(0);
            check(order.getOrderId() == saved.getOrderId(), "Sai mã đơn khi đọc từ tệp");
            check(order.getStatus() == OrderStatus.COOKING, "Sai trạng thái đơn khi đọc từ tệp");
            check("Không cay | nhiều hành".equals(order.getItems().get(0).getNote()), "Sai ghi chú có ký tự đặc biệt khi đọc từ tệp");
        } finally {
            Files.deleteIfExists(tempFile);
        }
        passed++;
    }

    private static void testViewsAndPermissionGuard() throws Exception {
        OrderService service = new RestaurantService();
        CartSession cart = new CartSession();
        cart.addProduct(service.getMenuItem(2), "<script>alert(1)</script>");

        String customerHtml = CustomerView.buildCustomerHtml(service.getMenu(), cart);
        check(!customerHtml.contains("<script>alert(1)</script>"), "View khách chưa chống XSS");
        check(customerHtml.contains("Cơm Gà Đặc Tuyển"), "Thiếu danh mục sinh tự động từ MenuCategory");

        Customer normalCustomer = new Customer("Khách", "Bàn 02", "");
        expectThrows(IllegalArgumentException.class,
                () -> new KitchenController(service, normalCustomer));

        RestaurantOwner owner = new RestaurantOwner("chef", "Bếp Trưởng");
        Order order = service.submitOrder(normalCustomer, cart);
        String kitchenHtml = KitchenView.buildOwnerHtml(service.getOrders(), owner, service.calculateTotalRevenue());
        check(kitchenHtml.contains("PHIẾU #" + order.getOrderId()), "KitchenView thiếu mã phiếu");
        check(kitchenHtml.contains("status-pending"), "KitchenView chưa tách class trạng thái CSS");
        passed++;
    }

    private static void testEndToEndHttpServers() throws Exception {
        OrderService service = new RestaurantService();
        RestaurantOwner owner = new RestaurantOwner("chef_admin", "Bếp Trưởng");
        HttpServer customerServer = HttpServer.create(new InetSocketAddress(0), 0);
        HttpServer kitchenServer = HttpServer.create(new InetSocketAddress(0), 0);
        try {
            new CustomerController(service).register(customerServer);
            new KitchenController(service, owner).register(kitchenServer);
            customerServer.start();
            kitchenServer.start();

            int cPort = customerServer.getAddress().getPort();
            int kPort = kitchenServer.getAddress().getPort();
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> homeRes = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + cPort + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            check(homeRes.statusCode() == 200, "Trang khách phải trả HTTP 200");
            String cookie = homeRes.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];

            HttpResponse<String> addRes = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + cPort + "/cart/add"))
                            .header("Cookie", cookie)
                            .POST(HttpRequest.BodyPublishers.ofString("id=1&note=Da+gion"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            check(addRes.statusCode() == 200 && addRes.body().contains("45.000"), "Thêm món qua HTTP thất bại");

            HttpResponse<String> submitRes = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + cPort + "/cart/submit"))
                            .header("Cookie", cookie)
                            .POST(HttpRequest.BodyPublishers.ofString("name=Minh+Quan&table=Ban+05&phone=0912345678"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            check(submitRes.statusCode() == 200 && submitRes.body().contains("#501"), "Gửi đơn qua HTTP thất bại");

            HttpResponse<String> statusRes = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + kPort + "/status"))
                            .POST(HttpRequest.BodyPublishers.ofString("orderId=501&status=COOKING"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            check(statusRes.statusCode() == 303, "Cập nhật trạng thái bếp phải redirect 303");
            check(service.getOrders().get(0).getStatus() == OrderStatus.COOKING, "Trạng thái đơn chưa chuyển sang COOKING");
        } finally {
            customerServer.stop(0);
            kitchenServer.stop(0);
        }
        passed++;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Kiểm thử thất bại: " + message);
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private static void expectThrows(Class<? extends Throwable> expectedType, ThrowingAction action) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                return;
            }
            throw new AssertionError("Kỳ vọng ngoại lệ " + expectedType.getSimpleName()
                    + " nhưng nhận được " + actual.getClass().getSimpleName(), actual);
        }
        throw new AssertionError("Kỳ vọng ném ngoại lệ " + expectedType.getSimpleName() + " nhưng không có lỗi nào xảy ra.");
    }
}
