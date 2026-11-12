package restaurant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import restaurant.model.CartSession;
import restaurant.model.Customer;
import restaurant.model.MenuItem;
import restaurant.model.Order;
import restaurant.model.OrderItem;
import restaurant.model.OrderStatus;

public final class RestaurantService {
    private static final List<MenuItem> MENU = List.of(
            // Danh mục: Cơm Gà Đặc Tuyển (com-ga)
            new MenuItem(1, "Cơm Gà Xối Mỡ Da Giòn", 45000, "com-ga",
                    "https://i.postimg.cc/Rh6DbvtZ/com-ga-xoi-mo.jpg",
                    "Đùi gà góc tư xối mỡ vàng ươm da giòn rụm, dùng kèm cơm chiên hạt tơi đảo bơ tỏi và sốt mắm kẹo."),
            new MenuItem(2, "Cơm Gà Hải Nam Chặt Phay", 48000, "com-ga",
                    "https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=600&h=420&fit=crop",
                    "Gà ta hấp lá chanh da giòn bóng mượt, cơm nấu từ nước cốt gà béo thơm kèm sốt gừng hành đặc trưng."),
            new MenuItem(3, "Cơm Gà Xé Phay Hội An", 45000, "com-ga",
                    "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=600&h=420&fit=crop",
                    "Cơm nghệ dẻo thơm nấu nước luộc gà, thịt gà ta xé phay trộn hành tây, rau răm và nước mạ đậm đà."),
            new MenuItem(4, "Cơm Gà Nướng Mật Ong Than Hoa", 52000, "com-ga",
                    "https://images.unsplash.com/photo-1532550907401-a500c9a57435?w=600&h=420&fit=crop",
                    "Đùi gà rút xương ướp mật ong rừng nướng xém cạnh thơm lừng, ăn kèm dưa góp chua ngọt và canh gà."),
            new MenuItem(5, "Cơm Gà Giòn Sốt Teriyaki", 46000, "com-ga",
                    "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=600&h=420&fit=crop",
                    "Phi lê đùi gà chiên giòn rụm phủ sốt Teriyaki sánh mịn, rắc mè trắng rang thơm và hành lá tươi."),

            // Danh mục: Món Gà Chế Biến (mon-ga)
            new MenuItem(6, "Gà Rán Giòn Truyền Thống", 38000, "mon-ga",
                    "https://i.postimg.cc/wMyGb9mC/hinh-anh-ga-ran.jpg",
                    "Lớp vỏ bột truyền thống chiên giòn tan màu vàng óng, giữ trọn thớ thịt gà mọng nước bên trong."),
            new MenuItem(7, "Gà Rán Sốt Cay Hàn Quốc", 44000, "mon-ga",
                    "https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=600&h=420&fit=crop",
                    "Miếng gà rán giòn áo đều lớp sốt Gochujang cay ngọt nồng nàn, phủ hạt mè trắng rang thơm."),
            new MenuItem(8, "Cánh Gà Chiên Nước Mắm Tỏi Phi", 48000, "mon-ga",
                    "https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=600&h=420&fit=crop",
                    "Cánh gà giữa chiên căng da, đảo nhanh cùng nước mắm nhĩ Phú Quốc và tỏi phi vàng giòn rụm."),
            new MenuItem(9, "Gà Ủ Muối Hoa Tiêu (Nửa Con)", 95000, "mon-ga",
                    "https://images.unsplash.com/photo-1569058242253-92a9c755a0ec?w=600&h=420&fit=crop",
                    "Gà ta ủ muối thảo mộc da vàng giòn sần sật, thịt hồng mềm ngọt chấm muối tiêu chanh ớt xanh."),
            new MenuItem(10, "Gà Viên Giòn Lắc Phô Mai", 35000, "mon-ga",
                    "https://images.unsplash.com/photo-1562967914-608f82629710?w=600&h=420&fit=crop",
                    "Thịt đùi gà cắt viên chiên vàng giòn rụm, lắc đều trong lớp bột phô mai cheddar béo mặn."),

            // Danh mục: Canh & Ăn Kèm (mon-kem)
            new MenuItem(11, "Súp Gà Nấm Hương Ngô Non", 25000, "mon-kem",
                    "https://images.unsplash.com/photo-1547592180-85f173990554?w=600&h=420&fit=crop",
                    "Súp nấu từ nước cốt xương gà sánh nhẹ, thịt gà xé sợi, ngô ngọt và nấm hương thơm dịu."),
            new MenuItem(12, "Khoai Tây Múi Cau Chiên Bơ", 26000, "mon-kem",
                    "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=600&h=420&fit=crop",
                    "Khoai tây tươi bổ múi cau chiên vỏ giòn ruột bùi, xóc bơ tỏi thơm lừng ăn kèm tương ớt."),
            new MenuItem(13, "Khoai Lang Kén Mè Đen", 25000, "mon-kem",
                    "https://i.postimg.cc/prh61PF3/dia-khoai-lang-ken.jpg",
                    "Khoai lang mật nghiền mịn bọc bột chiên phồng giòn rụm, dậy vị béo bùi của nước cốt dừa."),

            // Danh mục: Trà & Giải Khát (do-uong)
            new MenuItem(14, "Trà Đào Cam Sả Thanh Mát", 28000, "do-uong",
                    "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&h=420&fit=crop",
                    "Hồng trà ủ lạnh kết hợp cam vàng, sả tươi và đào miếng giòn ngọt giúp cân bằng vị giác."),
            new MenuItem(15, "Trà Tắc Xí Muội Mật Ong", 20000, "do-uong",
                    "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=600&h=420&fit=crop",
                    "Vị chua thanh của tắc tươi hòa quyện cùng xí muội mặn ngọt và mật ong rừng mát lành."),
            new MenuItem(16, "Trà Thảo Mộc La Hán Quả", 22000, "do-uong",
                    "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=600&h=420&fit=crop",
                    "Thức uống nấu thủ công từ la hán quả, bông cúc và táo đỏ thanh nhiệt khi dùng kèm món chiên nướng.")
    );

    private final List<Order> orders = new ArrayList<>();
    private int nextOrderId = 501;

    public List<MenuItem> getMenu() {
        return MENU;
    }

    public Optional<MenuItem> findMenuItem(int productId) {
        return MENU.stream().filter(item -> item.getId() == productId).findFirst();
    }

    public synchronized List<Order> getOrders() {
        return List.copyOf(orders);
    }

    public synchronized Order submitOrder(Customer customer, CartSession cart) {
        synchronized (cart) {
            List<OrderItem> items = cart.getItems();
            Order order = new Order(nextOrderId, customer, items);
            orders.add(0, order);
            nextOrderId++;
            cart.clear();
            return order;
        }
    }

    public synchronized boolean changeStatus(int orderId, OrderStatus status) {
        for (Order order : orders) {
            if (order.getOrderId() == orderId) {
                order.changeStatus(status);
                return true;
            }
        }
        return false;
    }

    public synchronized long calculateTotalRevenue() {
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.READY)
                .mapToLong(Order::calculateTotal)
                .sum();
    }
}
