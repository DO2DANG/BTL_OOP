import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;


// 1. DOMAIN MODEL 


abstract class User {
    private final String id;
    private final String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public abstract String getRoleName();
}

class Customer extends User {
    private final String tableNumber;
    private final String phone;

    public Customer(String name, String tableNumber, String phone) {
        super(UUID.randomUUID().toString().substring(0, 8), name);
        this.tableNumber = (tableNumber == null || tableNumber.isBlank()) ? "Bàn tự do" : tableNumber;
        this.phone = (phone == null || phone.isBlank()) ? "Chưa có SĐT" : phone;
    }

    public String getTableNumber() { return tableNumber; }
    public String getPhone() { return phone; }

    @Override
    public String getRoleName() {
        return "Khách hàng";
    }
}

class RestaurantOwner extends User {
    public RestaurantOwner(String username, String fullName) {
        super(username, fullName);
    }

    @Override
    public String getRoleName() {
        return "Quản trị / Chủ nhà hàng";
    }
}

class MenuItem {
    private final int id;
    private final String name;
    private final long price;
    private final String category;
    private final String imageUrl;
    private final String description;

    public MenuItem(int id, String name, long price, String category, String imageUrl, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public long getPrice() { return price; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
}

class OrderItem {
    private final MenuItem item;
    private int quantity;
    private String note;

    public OrderItem(MenuItem item, int quantity, String note) {
        this.item = item;
        this.quantity = quantity;
        this.note = (note != null) ? note.trim() : "";
    }

    public MenuItem getItem() { return item; }
    public int getQuantity() { return quantity; }
    public String getNote() { return note; }

    public void setNote(String note) { this.note = note; }
    public void increase() { this.quantity++; }
    public void decrease() { this.quantity--; }
    public long getSubtotal() { return item.getPrice() * quantity; }
}

enum OrderStatus {
    PENDING("Chờ chuẩn bị", "#d97706"),
    COOKING("Bếp đang chế biến", "#0284c7"),
    READY("Đã hoàn tất", "#16a34a"),
    CANCELLED("Đã hủy", "#dc2626");

    private final String title;
    private final String color;

    OrderStatus(String title, String color) {
        this.title = title;
        this.color = color;
    }

    public String getTitle() { return title; }
    public String getColor() { return color; }
}

class Order {
    private final int orderId;
    private final Customer customer;
    private final List<OrderItem> items;
    private final String createdAt;
    private OrderStatus status;

    public Order(int orderId, Customer customer, List<OrderItem> items) {
        this.orderId = orderId;
        this.customer = customer;
        this.items = new ArrayList<>(items);
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM"));
        this.status = OrderStatus.PENDING;
    }

    public int getOrderId() { return orderId; }
    public Customer getCustomer() { return customer; }
    public List<OrderItem> getItems() { return items; }
    public String getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public long calculateTotal() {
        return items.stream().mapToLong(OrderItem::getSubtotal).sum();
    }
}

class CartSession {
    private final List<OrderItem> items = new ArrayList<>();

    public synchronized void addProduct(MenuItem product, String note) {
        for (OrderItem item : items) {
            if (item.getItem().getId() == product.getId()) {
                item.increase();
                if (note != null && !note.isBlank()) item.setNote(note);
                return;
            }
        }
        items.add(new OrderItem(product, 1, note));
    }

    public synchronized void changeQty(int productId, int delta) {
        Iterator<OrderItem> it = items.iterator();
        while (it.hasNext()) {
            OrderItem item = it.next();
            if (item.getItem().getId() == productId) {
                if (delta > 0) {
                    item.increase();
                } else {
                    item.decrease();
                    if (item.getQuantity() <= 0) it.remove();
                }
                return;
            }
        }
    }

    public synchronized List<OrderItem> getItems() { return new ArrayList<>(items); }
    public synchronized int getTotalQuantity() { return items.stream().mapToInt(OrderItem::getQuantity).sum(); }
    public synchronized long getTotalPrice() { return items.stream().mapToLong(OrderItem::getSubtotal).sum(); }
    public synchronized void clear() { items.clear(); }
}

// 2. SERVICE & DATA REPOSITORY (CHUYÊN CÁC MÓN GÀ & CƠM GÀ)

class RestaurantService {
    private static final List<MenuItem> MENU = new ArrayList<>();
    private static final List<Order> ORDERS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(501);

    static {
        // Danh mục: Cơm Gà Đặc Tuyển (com-ga)
        MENU.add(new MenuItem(1, "Cơm Gà Xối Mỡ Da Giòn", 45000, "com-ga",
                "https://i.postimg.cc/Rh6DbvtZ/com-ga-xoi-mo.jpg",
                "Đùi gà góc tư xối mỡ vàng ươm da giòn rụm, dùng kèm cơm chiên hạt tơi đảo bơ tỏi và sốt mắm kẹo."));
        MENU.add(new MenuItem(2, "Cơm Gà Hải Nam Chặt Phay", 48000, "com-ga",
                "https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=600&h=420&fit=crop",
                "Gà ta hấp lá chanh da giòn bóng mượt, cơm nấu từ nước cốt gà béo thơm kèm sốt gừng hành đặc trưng."));
        MENU.add(new MenuItem(3, "Cơm Gà Xé Phay Hội An", 45000, "com-ga",
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=600&h=420&fit=crop",
                "Cơm nghệ dẻo thơm nấu nước luộc gà, thịt gà ta xé phay trộn hành tây, rau răm và nước mạ đậm đà."));
        MENU.add(new MenuItem(4, "Cơm Gà Nướng Mật Ong Than Hoa", 52000, "com-ga",
                "https://images.unsplash.com/photo-1532550907401-a500c9a57435?w=600&h=420&fit=crop",
                "Đùi gà rút xương ướp mật ong rừng nướng xém cạnh thơm lừng, ăn kèm dưa góp chua ngọt và canh gà."));
        MENU.add(new MenuItem(5, "Cơm Gà Giòn Sốt Teriyaki", 46000, "com-ga",
                "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=600&h=420&fit=crop",
                "Phi lê đùi gà chiên giòn rụm phủ sốt Teriyaki sánh mịn, rắc mè trắng rang thơm và hành lá tươi."));

        // Danh mục: Món Gà Chế Biến (mon-ga)
        MENU.add(new MenuItem(6, "Gà Rán Giòn Truyền Thống", 38000, "mon-ga",
                "https://i.postimg.cc/wMyGb9mC/hinh-anh-ga-ran.jpg",
                "Lớp vỏ bột truyền thống chiên giòn tan màu vàng óng, giữ trọn thớ thịt gà mọng nước bên trong."));
        MENU.add(new MenuItem(7, "Gà Rán Sốt Cay Hàn Quốc", 44000, "mon-ga",
                "https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=600&h=420&fit=crop",
                "Miếng gà rán giòn áo đều lớp sốt Gochujang cay ngọt nồng nàn, phủ hạt mè trắng rang thơm."));
        MENU.add(new MenuItem(8, "Cánh Gà Chiên Nước Mắm Tỏi Phi", 48000, "mon-ga",
                "https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=600&h=420&fit=crop",
                "Cánh gà giữa chiên căng da, đảo nhanh cùng nước mắm nhĩ Phú Quốc và tỏi phi vàng giòn rụm."));
        MENU.add(new MenuItem(9, "Gà Ủ Muối Hoa Tiêu (Nửa Con)", 95000, "mon-ga",
                "https://images.unsplash.com/photo-1569058242253-92a9c755a0ec?w=600&h=420&fit=crop",
                "Gà ta ủ muối thảo mộc da vàng giòn sần sật, thịt hồng mềm ngọt chấm muối tiêu chanh ớt xanh."));
        MENU.add(new MenuItem(10, "Gà Viên Giòn Lắc Phô Mai", 35000, "mon-ga",
                "https://images.unsplash.com/photo-1562967914-608f82629710?w=600&h=420&fit=crop",
                "Thịt đùi gà cắt viên chiên vàng giòn rụm, lắc đều trong lớp bột phô mai cheddar béo mặn."));

        // Danh mục: Canh & Ăn Kèm (mon-kem)
        MENU.add(new MenuItem(11, "Súp Gà Nấm Hương Ngô Non", 25000, "mon-kem",
                "https://images.unsplash.com/photo-1547592180-85f173990554?w=600&h=420&fit=crop",
                "Súp nấu từ nước cốt xương gà sánh nhẹ, thịt gà xé sợi, ngô ngọt và nấm hương thơm dịu."));
        MENU.add(new MenuItem(12, "Khoai Tây Múi Cau Chiên Bơ", 26000, "mon-kem",
                "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=600&h=420&fit=crop",
                "Khoai tây tươi bổ múi cau chiên vỏ giòn ruột bùi, xóc bơ tỏi thơm lừng ăn kèm tương ớt."));
        MENU.add(new MenuItem(13, "Khoai Lang Kén Mè Đen", 25000, "mon-kem",
                "https://i.postimg.cc/prh61PF3/dia-khoai-lang-ken.jpg",
                "Khoai lang mật nghiền mịn bọc bột chiên phồng giòn rụm, dậy vị béo bùi của nước cốt dừa."));

        // Danh mục: Trà & Giải Khát (do-uong)
        MENU.add(new MenuItem(14, "Trà Đào Cam Sả Thanh Mát", 28000, "do-uong",
                "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&h=420&fit=crop",
                "Hồng trà ủ lạnh kết hợp cam vàng, sả tươi và đào miếng giòn ngọt giúp cân bằng vị giác."));
        MENU.add(new MenuItem(15, "Trà Tắc Xí Muội Mật Ong", 20000, "do-uong",
                "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=600&h=420&fit=crop",
                "Vị chua thanh của tắc tươi hòa quyện cùng xí muội mặn ngọt và mật ong rừng mát lành."));
        MENU.add(new MenuItem(16, "Trà Thảo Mộc La Hán Quả", 22000, "do-uong",
                "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=600&h=420&fit=crop",
                "Thức uống nấu thủ công từ la hán quả, bông cúc và táo đỏ thanh nhiệt khi dùng kèm món chiên nướng."));
    }

    public static List<MenuItem> getMenu() { return MENU; }
    public static List<Order> getOrders() { return ORDERS; }

    public static synchronized Order submitOrder(Customer customer, CartSession cart) {
        if (cart.getItems().isEmpty()) return null;
        Order order = new Order(ID_GENERATOR.getAndIncrement(), customer, cart.getItems());
        ORDERS.add(0, order);
        cart.clear();
        return order;
    }

    public static synchronized boolean changeStatus(int orderId, OrderStatus status) {
        for (Order order : ORDERS) {
            if (order.getOrderId() == orderId) {
                order.setStatus(status);
                return true;
            }
        }
        return false;
    }

    public static long calculateTotalRevenue() {
        return ORDERS.stream()
                .filter(o -> o.getStatus() == OrderStatus.READY)
                .mapToLong(Order::calculateTotal)
                .sum();
    }
}


// 3. VIEW BUILDER: UI KHÁCH HÀNG & UI QUẢN TRỊ BẾP


class WebThemeBuilder {
    private static final NumberFormat MONEY = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    private static String getCategoryLabel(String categoryCode) {
        switch (categoryCode) {
            case "com-ga": return "Cơm Gà Đặc Tuyển";
            case "mon-ga": return "Món Gà Chế Biến";
            case "mon-kem": return "Canh & Ăn Kèm";
            case "do-uong": return "Giải Khát";
            default: return "Thực Đơn";
        }
    }

    public static String buildCartInnerHtml(CartSession cart, String notification) {
        StringBuilder sb = new StringBuilder();
        int totalQty = cart.getTotalQuantity();
        long totalPrice = cart.getTotalPrice();

        sb.append("<div class='cart-header' data-total-qty='").append(totalQty)
          .append("' data-total-price='").append(MONEY.format(totalPrice)).append(" đ'>");
        sb.append("<div>");
        sb.append("<span class='cart-eyebrow'>PHIẾU ĐẶT MÓN</span>");
        sb.append("<h2 class='cart-title'>Giỏ Hàng Của Bạn</h2>");
        sb.append("</div>");
        sb.append("<div class='cart-header-right'>");
        sb.append("<span class='cart-count-pill'>").append(totalQty).append(" phần</span>");
        sb.append("<button type='button' class='btn-close-drawer' onclick='toggleMobileCart(false)'>Đóng</button>");
        sb.append("</div>");
        sb.append("</div>");

        if (notification != null && !notification.isBlank()) {
            sb.append("<div class='order-banner'>").append(notification).append("</div>");
        }

        if (cart.getItems().isEmpty()) {
            sb.append("<div class='cart-empty'>");
            sb.append("<div class='empty-halo'></div>");
            sb.append("<p class='empty-title'>Chưa có món nào được chọn</p>");
            sb.append("<p class='empty-sub'>Chạm vào món gà hoặc cơm gà bất kỳ bên trái để bắt đầu lên đơn.</p>");
            sb.append("</div>");
        } else {
            sb.append("<div class='cart-items-list'>");
            for (OrderItem order : cart.getItems()) {
                sb.append("<div class='cart-item'>");
                sb.append("<div class='cart-item-info'>");
                sb.append("<div class='cart-item-name'>").append(order.getItem().getName()).append("</div>");
                sb.append("<div class='cart-item-price'>").append(MONEY.format(order.getItem().getPrice())).append(" đ</div>");
                if (!order.getNote().isBlank()) {
                    sb.append("<div class='cart-item-note'>Ghi chú: ").append(order.getNote()).append("</div>");
                }
                sb.append("</div>");

                sb.append("<div class='qty-controls'>");
                sb.append("<form onsubmit='submitAjax(event, this)' action='/cart/quantity' class='inline-form'>");
                sb.append("<input type='hidden' name='id' value='").append(order.getItem().getId()).append("'>");
                sb.append("<input type='hidden' name='delta' value='-1'>");
                sb.append("<button type='submit' class='btn-qty' aria-label='Giảm số lượng'>−</button>");
                sb.append("</form>");

                sb.append("<span class='qty-number'>").append(order.getQuantity()).append("</span>");

                sb.append("<form onsubmit='submitAjax(event, this)' action='/cart/quantity' class='inline-form'>");
                sb.append("<input type='hidden' name='id' value='").append(order.getItem().getId()).append("'>");
                sb.append("<input type='hidden' name='delta' value='1'>");
                sb.append("<button type='submit' class='btn-qty' aria-label='Tăng số lượng'>+</button>");
                sb.append("</form>");
                sb.append("</div></div>");
            }
            sb.append("</div>");

            sb.append("<div class='cart-summary'>");
            sb.append("<div class='summary-row'>");
            sb.append("<span>Tạm tính (").append(totalQty).append(" phần)</span>");
            sb.append("<strong class='summary-total'>").append(MONEY.format(totalPrice)).append(" đ</strong>");
            sb.append("</div>");
            sb.append("</div>");

            sb.append("<form onsubmit='submitAjax(event, this)' action='/cart/submit' class='checkout-form'>");
            sb.append("<div class='field-group'>");
            sb.append("<label class='field-label'>Tên thực khách</label>");
            sb.append("<input type='text' name='name' class='input-control' required placeholder='Nhập tên của bạn (VD: Minh Quân)'>");
            sb.append("</div>");
            sb.append("<div class='field-row'>");
            sb.append("<div class='field-group'>");
            sb.append("<label class='field-label'>Vị trí bàn / Hình thức</label>");
            sb.append("<input type='text' name='table' class='input-control' required placeholder='VD: Bàn 06 hoặc Mang về'>");
            sb.append("</div>");
            sb.append("<div class='field-group'>");
            sb.append("<label class='field-label'>Số điện thoại</label>");
            sb.append("<input type='tel' name='phone' class='input-control' placeholder='Không bắt buộc'>");
            sb.append("</div>");
            sb.append("</div>");
            sb.append("<button type='submit' class='btn-checkout'><span class='btn-checkout-text'>XÁC NHẬN ĐẶT MÓN</span></button>");
            sb.append("</form>");
        }
        return sb.toString();
    }

    public static String buildCustomerHtml(List<MenuItem> menu, CartSession cart) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0, viewport-fit=cover'>");
        sb.append("<title>Gà Vàng Phố Cổ — Chuyên Cơm Gà & Món Gà Thủ Công</title>");
        sb.append("<link rel='preconnect' href='https://fonts.googleapis.com'>");
        sb.append("<link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>");
        sb.append("<link href='https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Playfair+Display:ital,wght@0,600;0,700;0,800;1,600&display=swap' rel='stylesheet'>");
        sb.append("<style>");

        // Design System Tokens (Primitive -> Semantic -> Component)
        sb.append(":root {");
        sb.append("  --color-terracotta-700: #9a3412;");
        sb.append("  --color-terracotta-600: #c2410c;");
        sb.append("  --color-crimson-600: #dc2626;");
        sb.append("  --color-amber-500: #f59e0b;");
        sb.append("  --color-amber-400: #fbbf24;");
        sb.append("  --color-cream-50: #fbf8f3;");
        sb.append("  --color-cream-100: #f5efe4;");
        sb.append("  --color-espresso-950: #1c1008;");
        sb.append("  --color-espresso-800: #3f2718;");
        sb.append("  --color-stone-600: #6b5748;");
        sb.append("  --color-border-warm: rgba(194, 65, 12, 0.14);");
        sb.append("  --color-primary: var(--color-terracotta-600);");
        sb.append("  --color-primary-hover: var(--color-terracotta-700);");
        sb.append("  --color-accent: var(--color-amber-500);");
        sb.append("  --color-bg: var(--color-cream-50);");
        sb.append("  --color-surface: rgba(255, 255, 255, 0.88);");
        sb.append("  --color-text: var(--color-espresso-950);");
        sb.append("  --color-text-muted: var(--color-stone-600);");
        sb.append("  --radius-lg: 22px;");
        sb.append("  --radius-md: 14px;");
        sb.append("  --radius-pill: 999px;");
        sb.append("  --shadow-card: 0 12px 32px -8px rgba(63, 39, 24, 0.08), 0 2px 6px rgba(63, 39, 24, 0.04);");
        sb.append("  --shadow-hover: 0 22px 44px -10px rgba(194, 65, 12, 0.18), 0 4px 12px rgba(63, 39, 24, 0.06);");
        sb.append("  --ease-spring: cubic-bezier(0.22, 1, 0.36, 1);");
        sb.append("}");

        sb.append("* { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }");
        sb.append("html { scroll-behavior: smooth; }");
        sb.append("body {");
        sb.append("  font-family: 'Be Vietnam Pro', sans-serif;");
        sb.append("  background-color: var(--color-bg);");
        sb.append("  color: var(--color-text);");
        sb.append("  min-height: 100vh;");
        sb.append("  overflow-x: hidden;");
        sb.append("  position: relative;");
        sb.append("  line-height: 1.5;");
        sb.append("}");

        // Layered Atmospheric Background (No icons needed - rich visual texture & floating light orbs)
        sb.append(".ambient-canvas {");
        sb.append("  position: fixed; inset: 0; pointer-events: none; z-index: -1; overflow: hidden;");
        sb.append("  background-image:");
        sb.append("    radial-gradient(rgba(194, 65, 12, 0.07) 1px, transparent 1px),");
        sb.append("    radial-gradient(rgba(245, 158, 11, 0.05) 1px, transparent 1px);");
        sb.append("  background-size: 28px 28px;");
        sb.append("  background-position: 0 0, 14px 14px;");
        sb.append("}");
        sb.append(".ambient-orb {");
        sb.append("  position: absolute; border-radius: 50%; filter: blur(75px); opacity: 0.55;");
        sb.append("  animation: floatOrb 18s ease-in-out infinite alternate;");
        sb.append("}");
        sb.append(".orb-1 { width: 460px; height: 460px; background: radial-gradient(circle, rgba(245,158,11,0.28), transparent 70%); top: -120px; left: -100px; }");
        sb.append(".orb-2 { width: 520px; height: 520px; background: radial-gradient(circle, rgba(194,65,12,0.20), transparent 70%); top: 30%; right: -160px; animation-delay: -6s; }");
        sb.append(".orb-3 { width: 420px; height: 420px; background: radial-gradient(circle, rgba(217,119,6,0.22), transparent 70%); bottom: -100px; left: 20%; animation-delay: -11s; }");
        sb.append("@keyframes floatOrb {");
        sb.append("  0% { transform: translate3d(0, 0, 0) scale(1); }");
        sb.append("  50% { transform: translate3d(35px, -28px, 0) scale(1.08); }");
        sb.append("  100% { transform: translate3d(-25px, 22px, 0) scale(0.95); }");
        sb.append("}");

        // Hero Banner with layered light waves and editorial typography
        sb.append(".hero-header {");
        sb.append("  position: relative; overflow: hidden;");
        sb.append("  background: linear-gradient(132deg, #2a1205 0%, #6c220a 48%, #a8380b 82%, #d97706 100%);");
        sb.append("  color: #fff;");
        sb.append("  padding: 46px 24px 52px;");
        sb.append("  border-bottom: 1px solid rgba(251, 191, 36, 0.25);");
        sb.append("  box-shadow: 0 18px 40px -12px rgba(42, 18, 5, 0.35);");
        sb.append("}");
        sb.append(".hero-header::before {");
        sb.append("  content: ''; position: absolute; width: 580px; height: 580px; border-radius: 50%;");
        sb.append("  border: 1px solid rgba(251, 191, 36, 0.14); top: -290px; right: -140px; pointer-events: none;");
        sb.append("}");
        sb.append(".hero-header::after {");
        sb.append("  content: ''; position: absolute; inset: 0;");
        sb.append("  background: radial-gradient(circle at 80% 20%, rgba(251, 191, 36, 0.22), transparent 50%),");
        sb.append("              linear-gradient(90deg, rgba(0,0,0,0.25) 0%, transparent 60%);");
        sb.append("  pointer-events: none;");
        sb.append("}");
        sb.append(".hero-inner {");
        sb.append("  position: relative; z-index: 1; max-width: 1280px; margin: 0 auto;");
        sb.append("  display: flex; flex-direction: column; align-items: flex-start; gap: 14px;");
        sb.append("}");
        sb.append(".hero-badge {");
        sb.append("  display: inline-flex; align-items: center; gap: 10px;");
        sb.append("  padding: 6px 16px; border-radius: var(--radius-pill);");
        sb.append("  background: rgba(251, 191, 36, 0.16); border: 1px solid rgba(251, 191, 36, 0.45);");
        sb.append("  color: #fde68a; font-size: 0.78rem; font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase;");
        sb.append("  backdrop-filter: blur(8px);");
        sb.append("}");
        sb.append(".hero-badge-dot {");
        sb.append("  width: 8px; height: 8px; border-radius: 50%; background: #fbbf24;");
        sb.append("  box-shadow: 0 0 12px #fbbf24; animation: pulseDot 2s infinite;");
        sb.append("}");
        sb.append("@keyframes pulseDot { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.4); opacity: 0.6; } }");
        sb.append(".brand-title {");
        sb.append("  font-family: 'Playfair Display', serif; font-size: clamp(2rem, 4.5vw, 3.35rem);");
        sb.append("  font-weight: 800; line-height: 1.12; letter-spacing: -0.01em; color: #fffbeb;");
        sb.append("}");
        sb.append(".brand-title span {");
        sb.append("  background: linear-gradient(90deg, #fde68a, #f59e0b);");
        sb.append("  -webkit-background-clip: text; -webkit-text-fill-color: transparent;");
        sb.append("}");
        sb.append(".brand-sub {");
        sb.append("  font-size: clamp(0.95rem, 1.8vw, 1.1rem); color: rgba(254, 243, 199, 0.88);");
        sb.append("  max-width: 640px; font-weight: 400;");
        sb.append("}");
        sb.append(".hero-highlights {");
        sb.append("  display: flex; flex-wrap: wrap; gap: 10px; margin-top: 6px;");
        sb.append("}");
        sb.append(".highlight-chip {");
        sb.append("  padding: 7px 14px; border-radius: 10px; font-size: 0.82rem; font-weight: 600;");
        sb.append("  background: rgba(255, 255, 255, 0.09); border: 1px solid rgba(255, 255, 255, 0.16);");
        sb.append("  color: #fef3c7; backdrop-filter: blur(6px); transition: transform 0.25s var(--ease-spring), background 0.25s;");
        sb.append("}");
        sb.append(".highlight-chip:hover { transform: translateY(-2px); background: rgba(255, 255, 255, 0.16); }");

        // Sticky Category Navigation with Horizontal Touch Snap on Mobile
        sb.append(".category-wrap {");
        sb.append("  position: sticky; top: 0; z-index: 40;");
        sb.append("  background: rgba(251, 248, 243, 0.84);");
        sb.append("  backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);");
        sb.append("  border-bottom: 1px solid var(--color-border-warm);");
        sb.append("  padding: 14px 0;");
        sb.append("  transition: box-shadow 0.25s ease;");
        sb.append("}");
        sb.append(".category-bar {");
        sb.append("  max-width: 1280px; margin: 0 auto; padding: 0 20px;");
        sb.append("  display: flex; align-items: center; gap: 10px;");
        sb.append("  overflow-x: auto; scroll-snap-type: x mandatory; scrollbar-width: none;");
        sb.append("}");
        sb.append(".category-bar::-webkit-scrollbar { display: none; }");
        sb.append(".cat-pill {");
        sb.append("  position: relative; overflow: hidden; scroll-snap-align: start; flex-shrink: 0;");
        sb.append("  min-height: 44px; padding: 10px 22px; border-radius: var(--radius-pill);");
        sb.append("  font-family: inherit; font-weight: 700; font-size: 0.92rem; color: var(--color-espresso-800);");
        sb.append("  background: rgba(255, 255, 255, 0.85); border: 1.5px solid rgba(194, 65, 12, 0.15);");
        sb.append("  cursor: pointer; transition: all 0.3s var(--ease-spring); outline: none;");
        sb.append("  box-shadow: 0 2px 8px rgba(63, 39, 24, 0.04);");
        sb.append("}");
        sb.append(".cat-pill:hover {");
        sb.append("  border-color: var(--color-amber-500); transform: translateY(-2px);");
        sb.append("  box-shadow: 0 8px 18px rgba(217, 119, 6, 0.15);");
        sb.append("}");
        sb.append(".cat-pill:active { transform: scale(0.96); }");
        sb.append(".cat-pill.active {");
        sb.append("  background: linear-gradient(135deg, var(--color-terracotta-600), #ea580c);");
        sb.append("  color: #fff; border-color: transparent;");
        sb.append("  box-shadow: 0 10px 22px -4px rgba(194, 65, 12, 0.42);");
        sb.append("}");

        // Main Workspace Layout
        sb.append(".main-layout {");
        sb.append("  max-width: 1280px; margin: 28px auto 0; padding: 0 20px 80px;");
        sb.append("  display: grid; grid-template-columns: 1fr 380px; gap: 32px; align-items: start;");
        sb.append("}");

        // Food Menu Grid & Interactive Spotlight Cards
        sb.append(".menu-grid {");
        sb.append("  display: grid; grid-template-columns: repeat(auto-fill, minmax(265px, 1fr)); gap: 24px;");
        sb.append("}");
        sb.append(".food-card {");
        sb.append("  --mx: 50%; --my: 50%;");
        sb.append("  position: relative; background: var(--color-surface);");
        sb.append("  border-radius: var(--radius-lg); overflow: hidden;");
        sb.append("  border: 1.5px solid rgba(194, 65, 12, 0.12);");
        sb.append("  box-shadow: var(--shadow-card);");
        sb.append("  display: flex; flex-direction: column; justify-content: space-between;");
        sb.append("  transition: transform 0.35s var(--ease-spring), box-shadow 0.35s var(--ease-spring), border-color 0.3s ease;");
        sb.append("  animation: cardReveal 0.48s var(--ease-spring) backwards;");
        sb.append("  backdrop-filter: blur(10px);");
        sb.append("}");
        sb.append(".food-card::before {");
        sb.append("  content: ''; position: absolute; inset: 0; pointer-events: none; z-index: 1; opacity: 0;");
        sb.append("  background: radial-gradient(360px circle at var(--mx) var(--my), rgba(245, 158, 11, 0.16), transparent 70%);");
        sb.append("  transition: opacity 0.3s ease;");
        sb.append("}");
        sb.append(".food-card:hover {");
        sb.append("  transform: translateY(-7px);");
        sb.append("  box-shadow: var(--shadow-hover);");
        sb.append("  border-color: rgba(245, 158, 11, 0.55);");
        sb.append("}");
        sb.append(".food-card:hover::before { opacity: 1; }");
        sb.append("@keyframes cardReveal {");
        sb.append("  from { opacity: 0; transform: translateY(18px) scale(0.97); }");
        sb.append("  to { opacity: 1; transform: translateY(0) scale(1); }");
        sb.append("}");

        // Image Frame with Warm Gradient Vignette & Category Ribbon
        sb.append(".food-media { position: relative; width: 100%; height: 195px; overflow: hidden; background: #ebd9c8; }");
        sb.append(".food-img {");
        sb.append("  width: 100%; height: 100%; object-fit: cover;");
        sb.append("  transition: transform 0.55s var(--ease-spring), filter 0.4s ease;");
        sb.append("}");
        sb.append(".food-card:hover .food-img { transform: scale(1.07); filter: saturate(1.08); }");
        sb.append(".food-media::after {");
        sb.append("  content: ''; position: absolute; inset: 0;");
        sb.append("  background: linear-gradient(to top, rgba(28, 16, 8, 0.62) 0%, rgba(28, 16, 8, 0.0) 55%);");
        sb.append("  pointer-events: none;");
        sb.append("}");
        sb.append(".food-cat-tag {");
        sb.append("  position: absolute; top: 12px; left: 12px; z-index: 2;");
        sb.append("  padding: 5px 12px; border-radius: var(--radius-pill);");
        sb.append("  background: rgba(28, 16, 8, 0.72); color: #fde68a;");
        sb.append("  font-size: 0.72rem; font-weight: 700; letter-spacing: 0.05em; text-transform: uppercase;");
        sb.append("  backdrop-filter: blur(8px); border: 1px solid rgba(251, 191, 36, 0.3);");
        sb.append("}");
        sb.append(".food-price-badge {");
        sb.append("  position: absolute; bottom: 12px; right: 12px; z-index: 2;");
        sb.append("  padding: 6px 14px; border-radius: 12px;");
        sb.append("  background: linear-gradient(135deg, #fffbeb, #fef3c7);");
        sb.append("  color: var(--color-terracotta-700); font-weight: 800; font-size: 1.02rem;");
        sb.append("  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.22);");
        sb.append("}");

        // Card Body & Interactive Form
        sb.append(".food-info {");
        sb.append("  position: relative; z-index: 2; padding: 18px; flex-grow: 1;");
        sb.append("  display: flex; flex-direction: column; justify-content: space-between; gap: 14px;");
        sb.append("}");
        sb.append(".food-title {");
        sb.append("  font-family: 'Playfair Display', serif; font-size: 1.24rem; font-weight: 700;");
        sb.append("  color: var(--color-espresso-950); margin-bottom: 6px; line-height: 1.3;");
        sb.append("}");
        sb.append(".food-desc {");
        sb.append("  font-size: 0.87rem; color: var(--color-text-muted); line-height: 1.5;");
        sb.append("  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;");
        sb.append("}");
        sb.append(".food-note {");
        sb.append("  width: 100%; min-height: 42px; padding: 9px 13px;");
        sb.append("  border: 1.5px solid rgba(194, 65, 12, 0.16); border-radius: 12px;");
        sb.append("  font-family: inherit; font-size: 0.85rem; color: var(--color-text);");
        sb.append("  background: rgba(251, 248, 243, 0.75); margin-bottom: 10px; outline: none;");
        sb.append("  transition: border-color 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;");
        sb.append("}");
        sb.append(".food-note:focus {");
        sb.append("  border-color: var(--color-primary); background: #fff;");
        sb.append("  box-shadow: 0 0 0 4px rgba(194, 65, 12, 0.12);");
        sb.append("}");
        sb.append(".btn-add-cart {");
        sb.append("  position: relative; overflow: hidden; width: 100%; min-height: 46px;");
        sb.append("  background: linear-gradient(135deg, var(--color-terracotta-600), #ea580c);");
        sb.append("  color: #fff; border: none; padding: 12px 16px; border-radius: 13px;");
        sb.append("  font-family: inherit; font-weight: 700; font-size: 0.92rem; letter-spacing: 0.03em;");
        sb.append("  cursor: pointer; transition: transform 0.22s var(--ease-spring), box-shadow 0.25s ease, background 0.25s ease;");
        sb.append("  box-shadow: 0 6px 16px -4px rgba(194, 65, 12, 0.38);");
        sb.append("}");
        sb.append(".btn-add-cart::after {");
        sb.append("  content: ''; position: absolute; top: 0; left: -100%; width: 60%; height: 100%;");
        sb.append("  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.28), transparent);");
        sb.append("  transform: skewX(-20deg); transition: left 0.55s ease;");
        sb.append("}");
        sb.append(".btn-add-cart:hover { transform: translateY(-2px); box-shadow: 0 10px 22px -4px rgba(194, 65, 12, 0.52); }");
        sb.append(".btn-add-cart:hover::after { left: 140%; }");
        sb.append(".btn-add-cart:active { transform: scale(0.96); }");
        sb.append(".btn-add-cart.added-state { background: linear-gradient(135deg, #15803d, #16a34a); }");

        // Cart Panel (Sticky Desktop / Bottom Sheet Drawer on Mobile)
        sb.append(".cart-container {");
        sb.append("  background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(16px);");
        sb.append("  border-radius: var(--radius-lg); padding: 24px;");
        sb.append("  box-shadow: 0 16px 40px -10px rgba(63, 39, 24, 0.12);");
        sb.append("  border: 1.5px solid rgba(194, 65, 12, 0.16);");
        sb.append("  position: sticky; top: 84px;");
        sb.append("  max-height: calc(100vh - 104px); overflow-y: auto;");
        sb.append("  transition: transform 0.38s var(--ease-spring), box-shadow 0.3s ease;");
        sb.append("}");
        sb.append(".cart-container.cart-bump { animation: cartPulse 0.42s var(--ease-spring); }");
        sb.append("@keyframes cartPulse { 0% { transform: scale(1); } 40% { transform: scale(1.025); } 100% { transform: scale(1); } }");
        sb.append(".cart-header {");
        sb.append("  display: flex; justify-content: space-between; align-items: flex-start;");
        sb.append("  padding-bottom: 14px; margin-bottom: 16px;");
        sb.append("  border-bottom: 1.5px dashed rgba(194, 65, 12, 0.22);");
        sb.append("}");
        sb.append(".cart-eyebrow { font-size: 0.72rem; font-weight: 800; letter-spacing: 0.1em; color: var(--color-primary); display: block; margin-bottom: 2px; }");
        sb.append(".cart-title { font-family: 'Playfair Display', serif; font-size: 1.45rem; color: var(--color-espresso-950); }");
        sb.append(".cart-header-right { display: flex; align-items: center; gap: 8px; }");
        sb.append(".cart-count-pill {");
        sb.append("  background: linear-gradient(135deg, var(--color-terracotta-600), #ea580c);");
        sb.append("  color: #fff; font-size: 0.8rem; font-weight: 700; padding: 6px 13px; border-radius: var(--radius-pill);");
        sb.append("}");
        sb.append(".btn-close-drawer {");
        sb.append("  display: none; min-height: 38px; padding: 6px 14px; border-radius: var(--radius-pill);");
        sb.append("  border: 1.5px solid rgba(194, 65, 12, 0.25); background: var(--color-cream-100);");
        sb.append("  color: var(--color-espresso-800); font-weight: 700; font-size: 0.82rem; cursor: pointer;");
        sb.append("}");

        // Empty Cart State with Animated Halo (No emoji)
        sb.append(".cart-empty { text-align: center; padding: 38px 16px; }");
        sb.append(".empty-halo {");
        sb.append("  width: 68px; height: 68px; margin: 0 auto 16px; border-radius: 50%;");
        sb.append("  background: radial-gradient(circle, rgba(245, 158, 11, 0.28) 0%, rgba(194, 65, 12, 0.08) 65%, transparent 100%);");
        sb.append("  border: 2px dashed rgba(194, 65, 12, 0.35);");
        sb.append("  animation: spinSlow 14s linear infinite;");
        sb.append("}");
        sb.append("@keyframes spinSlow { to { transform: rotate(360deg); } }");
        sb.append(".empty-title { font-weight: 700; color: var(--color-espresso-800); margin-bottom: 6px; font-size: 1rem; }");
        sb.append(".empty-sub { font-size: 0.85rem; color: var(--color-text-muted); line-height: 1.5; }");

        // Cart Items & Tactile Quantity Stepper (Touch target >= 44px on mobile)
        sb.append(".cart-items-list { display: flex; flex-direction: column; gap: 10px; max-height: 310px; overflow-y: auto; padding-right: 4px; }");
        sb.append(".cart-item {");
        sb.append("  display: flex; justify-content: space-between; align-items: center; gap: 12px;");
        sb.append("  padding: 12px 14px; border-radius: 14px; background: var(--color-cream-50);");
        sb.append("  border: 1px solid rgba(194, 65, 12, 0.1); transition: background 0.2s ease, transform 0.2s ease;");
        sb.append("}");
        sb.append(".cart-item:hover { background: #fff7ed; }");
        sb.append(".cart-item-info { flex: 1; min-width: 0; }");
        sb.append(".cart-item-name { font-weight: 700; color: var(--color-espresso-950); font-size: 0.94rem; margin-bottom: 2px; }");
        sb.append(".cart-item-price { color: var(--color-primary); font-weight: 700; font-size: 0.86rem; }");
        sb.append(".cart-item-note { font-size: 0.78rem; color: #9a3412; font-style: italic; margin-top: 4px; background: rgba(245, 158, 11, 0.14); padding: 3px 8px; border-radius: 6px; display: inline-block; }");
        sb.append(".qty-controls { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }");
        sb.append(".inline-form { display: inline-flex; }");
        sb.append(".btn-qty {");
        sb.append("  position: relative; overflow: hidden; width: 34px; height: 34px; border-radius: 10px;");
        sb.append("  border: 1.5px solid rgba(194, 65, 12, 0.28); background: #fff; color: var(--color-primary);");
        sb.append("  font-weight: 800; font-size: 1.05rem; cursor: pointer; transition: all 0.2s var(--ease-spring);");
        sb.append("  display: inline-flex; align-items: center; justify-content: center;");
        sb.append("}");
        sb.append(".btn-qty:hover { background: var(--color-primary); color: #fff; border-color: var(--color-primary); transform: translateY(-1px); }");
        sb.append(".btn-qty:active { transform: scale(0.9); }");
        sb.append(".qty-number { min-width: 22px; text-align: center; font-weight: 800; font-size: 0.95rem; }");

        // Cart Summary & Checkout Form
        sb.append(".cart-summary {");
        sb.append("  margin: 18px 0; padding: 14px 16px; border-radius: 14px;");
        sb.append("  background: linear-gradient(135deg, #fff7ed, #ffedd5);");
        sb.append("  border: 1px solid rgba(245, 158, 11, 0.35);");
        sb.append("}");
        sb.append(".summary-row { display: flex; justify-content: space-between; align-items: center; font-size: 0.95rem; color: var(--color-espresso-800); }");
        sb.append(".summary-total { font-size: 1.3rem; font-weight: 800; color: var(--color-terracotta-700); }");
        sb.append(".checkout-form { display: flex; flex-direction: column; gap: 12px; }");
        sb.append(".field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }");
        sb.append(".field-group { display: flex; flex-direction: column; gap: 5px; }");
        sb.append(".field-label { font-size: 0.78rem; font-weight: 700; color: var(--color-espresso-800); }");
        sb.append(".input-control {");
        sb.append("  width: 100%; min-height: 44px; padding: 10px 14px;");
        sb.append("  border: 1.5px solid rgba(194, 65, 12, 0.2); border-radius: 12px;");
        sb.append("  font-family: inherit; font-size: 0.9rem; color: var(--color-text); background: #fff; outline: none;");
        sb.append("  transition: border-color 0.25s ease, box-shadow 0.25s ease;");
        sb.append("}");
        sb.append(".input-control:focus { border-color: var(--color-primary); box-shadow: 0 0 0 4px rgba(194, 65, 12, 0.12); }");
        sb.append(".btn-checkout {");
        sb.append("  position: relative; overflow: hidden; width: 100%; min-height: 50px; margin-top: 4px;");
        sb.append("  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);");
        sb.append("  color: #1c1008; border: none; border-radius: 14px; padding: 14px;");
        sb.append("  font-family: inherit; font-size: 1rem; font-weight: 800; letter-spacing: 0.04em;");
        sb.append("  cursor: pointer; box-shadow: 0 10px 24px -6px rgba(217, 119, 6, 0.55);");
        sb.append("  transition: transform 0.25s var(--ease-spring), box-shadow 0.25s ease;");
        sb.append("}");
        sb.append(".btn-checkout:hover { transform: translateY(-2px); box-shadow: 0 14px 28px -6px rgba(217, 119, 6, 0.7); }");
        sb.append(".btn-checkout:active { transform: scale(0.97); }");

        // Order Banner & Interactive Ripple / Flying Particle / Toast
        sb.append(".order-banner {");
        sb.append("  background: linear-gradient(135deg, #ecfdf5, #d1fae5); color: #065f46;");
        sb.append("  padding: 14px 16px; border-radius: 14px; margin-bottom: 16px;");
        sb.append("  font-weight: 700; font-size: 0.9rem; border-left: 5px solid #10b981;");
        sb.append("  animation: cardReveal 0.35s var(--ease-spring);");
        sb.append("}");
        sb.append(".ripple-wave {");
        sb.append("  position: absolute; border-radius: 50%; transform: scale(0); pointer-events: none;");
        sb.append("  background: rgba(255, 255, 255, 0.45); animation: rippleAnim 0.6s ease-out forwards;");
        sb.append("}");
        sb.append("@keyframes rippleAnim { to { transform: scale(4); opacity: 0; } }");
        sb.append(".fly-orb {");
        sb.append("  position: fixed; z-index: 9999; pointer-events: none;");
        sb.append("  padding: 6px 12px; border-radius: var(--radius-pill);");
        sb.append("  background: linear-gradient(135deg, #f59e0b, #c2410c); color: #fff;");
        sb.append("  font-weight: 800; font-size: 0.82rem; box-shadow: 0 8px 20px rgba(194, 65, 12, 0.45);");
        sb.append("  transition: transform 0.62s cubic-bezier(0.2, 0.8, 0.2, 1), opacity 0.62s ease;");
        sb.append("}");
        sb.append("#toast-popup {");
        sb.append("  position: fixed; bottom: 28px; right: 28px; z-index: 1200;");
        sb.append("  background: #1c1008; color: #fffbeb; padding: 14px 22px; border-radius: 14px;");
        sb.append("  box-shadow: 0 16px 36px rgba(28, 16, 8, 0.32); border-left: 4px solid var(--color-amber-400);");
        sb.append("  font-weight: 600; font-size: 0.92rem; overflow: hidden;");
        sb.append("  transform: translateY(20px); opacity: 0; pointer-events: none;");
        sb.append("  transition: transform 0.3s var(--ease-spring), opacity 0.3s ease;");
        sb.append("}");
        sb.append("#toast-popup.show { transform: translateY(0); opacity: 1; }");
        sb.append(".toast-progress {");
        sb.append("  position: absolute; bottom: 0; left: 0; height: 3px; width: 100%;");
        sb.append("  background: linear-gradient(90deg, #fbbf24, #ea580c); transform-origin: left;");
        sb.append("  animation: toastBar 2.3s linear forwards;");
        sb.append("}");
        sb.append("@keyframes toastBar { from { transform: scaleX(1); } to { transform: scaleX(0); } }");

        // Mobile Bottom Bar & Scrim Overlay (Hidden on Desktop)
        sb.append(".cart-backdrop {");
        sb.append("  position: fixed; inset: 0; background: rgba(28, 16, 8, 0.58);");
        sb.append("  backdrop-filter: blur(4px); z-index: 90; opacity: 0; pointer-events: none;");
        sb.append("  transition: opacity 0.32s ease;");
        sb.append("}");
        sb.append(".cart-backdrop.open { opacity: 1; pointer-events: auto; }");
        sb.append(".mobile-cart-bar {");
        sb.append("  display: none; position: fixed; left: 14px; right: 14px;");
        sb.append("  bottom: calc(14px + env(safe-area-inset-bottom, 0px)); z-index: 85;");
        sb.append("  background: linear-gradient(135deg, #2a1205 0%, #7c2d12 100%);");
        sb.append("  color: #fff; border-radius: 18px; padding: 12px 18px;");
        sb.append("  box-shadow: 0 16px 36px rgba(28, 16, 8, 0.42);");
        sb.append("  border: 1px solid rgba(251, 191, 36, 0.35);");
        sb.append("  align-items: center; justify-content: space-between; cursor: pointer;");
        sb.append("  transition: transform 0.25s var(--ease-spring);");
        sb.append("}");
        sb.append(".mobile-cart-bar:active { transform: scale(0.97); }");
        sb.append(".mobile-bar-left { display: flex; flex-direction: column; }");
        sb.append(".mobile-bar-qty { font-size: 0.76rem; color: #fde68a; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; }");
        sb.append(".mobile-bar-price { font-size: 1.15rem; font-weight: 800; color: #fff; }");
        sb.append(".mobile-bar-cta {");
        sb.append("  background: linear-gradient(135deg, #fbbf24, #f59e0b); color: #1c1008;");
        sb.append("  padding: 10px 18px; border-radius: 12px; font-weight: 800; font-size: 0.88rem;");
        sb.append("}");

        // Responsive Breakpoints (Tablet & Mobile Optimization)
        sb.append("@media (max-width: 960px) {");
        sb.append("  .main-layout { grid-template-columns: 1fr; padding-bottom: 120px; }");
        sb.append("  .mobile-cart-bar { display: flex; }");
        sb.append("  .btn-close-drawer { display: inline-flex; align-items: center; }");
        sb.append("  .cart-container {");
        sb.append("    position: fixed; left: 0; right: 0; bottom: 0; top: auto; z-index: 100;");
        sb.append("    width: 100%; max-height: 84vh; border-radius: 26px 26px 0 0;");
        sb.append("    padding: 22px 20px calc(24px + env(safe-area-inset-bottom, 0px));");
        sb.append("    transform: translateY(105%); box-shadow: 0 -16px 48px rgba(28, 16, 8, 0.35);");
        sb.append("  }");
        sb.append("  .cart-container.drawer-open { transform: translateY(0); }");
        sb.append("  #toast-popup { left: 16px; right: 16px; bottom: 92px; }");
        sb.append("}");

        sb.append("@media (max-width: 600px) {");
        sb.append("  .hero-header { padding: 32px 16px 36px; }");
        sb.append("  .category-bar { padding: 0 14px; gap: 8px; }");
        sb.append("  .cat-pill { padding: 9px 18px; font-size: 0.86rem; }");
        sb.append("  .main-layout { padding: 0 14px 120px; margin-top: 18px; gap: 18px; }");
        sb.append("  .menu-grid { grid-template-columns: 1fr; gap: 16px; }");
        sb.append("  .food-media { height: 180px; }");
        sb.append("  .btn-qty { width: 42px; height: 42px; font-size: 1.15rem; }");
        sb.append("  .field-row { grid-template-columns: 1fr; }");
        sb.append("}");

        sb.append("@media (prefers-reduced-motion: reduce) {");
        sb.append("  *, *::before, *::after { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }");
        sb.append("}");
        sb.append("</style></head><body>");

        // Ambient Layered Background
        sb.append("<div class='ambient-canvas' aria-hidden='true'>");
        sb.append("<div class='ambient-orb orb-1'></div>");
        sb.append("<div class='ambient-orb orb-2'></div>");
        sb.append("<div class='ambient-orb orb-3'></div>");
        sb.append("</div>");

        // Hero Header
        sb.append("<header class='hero-header'>");
        sb.append("<div class='hero-inner'>");
        sb.append("<div class='hero-badge'><span class='hero-badge-dot'></span>TIỆM CƠM GÀ & MÓN GÀ THỦ CÔNG</div>");
        sb.append("<h1 class='brand-title'>GÀ VÀNG <span>PHỐ CỔ</span></h1>");
        sb.append("<p class='brand-sub'>Tinh hoa ẩm thực gà ta tuyển chọn: Cơm gà xối mỡ da giòn, gà hấp lá chanh, gà ủ muối hoa tiêu và các món gà chế biến nóng hổi phục vụ tại bàn.</p>");
        sb.append("<div class='hero-highlights'>");
        sb.append("<span class='highlight-chip'>Gà Ta Tươi Trong Ngày</span>");
        sb.append("<span class='highlight-chip'>Nước Sốt Gia Truyền</span>");
        sb.append("<span class='highlight-chip'>Lên Đơn Trực Tiếp Vào Bếp</span>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</header>");

        // Sticky Category Filter Bar
        sb.append("<div class='category-wrap'>");
        sb.append("<nav class='category-bar' aria-label='Danh mục thực đơn'>");
        sb.append("<button type='button' class='cat-pill active' onclick=\"filterCategory('all', this, event)\">Tất Cả Thực Đơn</button>");
        sb.append("<button type='button' class='cat-pill' onclick=\"filterCategory('com-ga', this, event)\">Cơm Gà Đặc Tuyển</button>");
        sb.append("<button type='button' class='cat-pill' onclick=\"filterCategory('mon-ga', this, event)\">Món Gà Chế Biến</button>");
        sb.append("<button type='button' class='cat-pill' onclick=\"filterCategory('mon-kem', this, event)\">Canh & Ăn Kèm</button>");
        sb.append("<button type='button' class='cat-pill' onclick=\"filterCategory('do-uong', this, event)\">Trà & Giải Khát</button>");
        sb.append("</nav>");
        sb.append("</div>");

        sb.append("<main class='main-layout'>");

        // Menu Grid
        sb.append("<div class='menu-grid' id='menu-grid'>");
        int idx = 0;
        for (MenuItem item : menu) {
            sb.append("<article class='food-card' data-category='").append(item.getCategory())
              .append("' style='animation-delay: ").append(idx * 45).append("ms;' onmousemove='handleCardSpotlight(event, this)'>");
            sb.append("<div class='food-media'>");
            sb.append("<span class='food-cat-tag'>").append(getCategoryLabel(item.getCategory())).append("</span>");
            sb.append("<img class='food-img' src='").append(item.getImageUrl()).append("' alt='").append(item.getName())
              .append("' loading='lazy' onerror=\"this.src='https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=600&h=420&fit=crop';\">");
            sb.append("<span class='food-price-badge'>").append(MONEY.format(item.getPrice())).append(" đ</span>");
            sb.append("</div>");

            sb.append("<div class='food-info'>");
            sb.append("<div>");
            sb.append("<h3 class='food-title'>").append(item.getName()).append("</h3>");
            sb.append("<p class='food-desc'>").append(item.getDescription()).append("</p>");
            sb.append("</div>");

            sb.append("<form onsubmit='submitAjax(event, this)' action='/cart/add'>");
            sb.append("<input type='hidden' name='id' value='").append(item.getId()).append("'>");
            sb.append("<input type='text' name='note' class='food-note' placeholder='Ghi chú (VD: Nhiều sốt, ít hành, thêm ớt...)'>");
            sb.append("<button type='submit' class='btn-add-cart' onclick='spawnRipple(event, this)'>THÊM VÀO ĐƠN</button>");
            sb.append("</form>");
            sb.append("</div></article>");
            idx++;
        }
        sb.append("</div>");

        // Cart Container (Desktop Sticky / Mobile Bottom Drawer)
        sb.append("<aside id='cart-box' class='cart-container' aria-label='Giỏ hàng'>");
        sb.append(buildCartInnerHtml(cart, ""));
        sb.append("</aside>");

        sb.append("</main>");

        // Mobile Scrim & Sticky Bottom Bar
        sb.append("<div id='cart-backdrop' class='cart-backdrop' onclick='toggleMobileCart(false)'></div>");
        sb.append("<div id='mobile-cart-bar' class='mobile-cart-bar' onclick='toggleMobileCart(true)'>");
        sb.append("<div class='mobile-bar-left'>");
        sb.append("<span id='mobile-bar-qty' class='mobile-bar-qty'>").append(cart.getTotalQuantity()).append(" phần trong giỏ</span>");
        sb.append("<span id='mobile-bar-price' class='mobile-bar-price'>").append(MONEY.format(cart.getTotalPrice())).append(" đ</span>");
        sb.append("</div>");
        sb.append("<span class='mobile-bar-cta'>Xem Giỏ & Đặt Món</span>");
        sb.append("</div>");

        // Toast Notification
        sb.append("<div id='toast-popup' role='status' aria-live='polite'></div>");

        // Interactive Scripts (Spotlight, Ripple, Flying Orb, Category Stagger, Mobile Drawer & AJAX)
        sb.append("<script>");
        sb.append("function handleCardSpotlight(e, card) {");
        sb.append("  const rect = card.getBoundingClientRect();");
        sb.append("  card.style.setProperty('--mx', (e.clientX - rect.left) + 'px');");
        sb.append("  card.style.setProperty('--my', (e.clientY - rect.top) + 'px');");
        sb.append("}");

        sb.append("function spawnRipple(e, btn) {");
        sb.append("  if (!btn) return;");
        sb.append("  const rect = btn.getBoundingClientRect();");
        sb.append("  const wave = document.createElement('span');");
        sb.append("  const size = Math.max(rect.width, rect.height);");
        sb.append("  wave.style.width = wave.style.height = size + 'px';");
        sb.append("  wave.style.left = ((e.clientX || rect.left + rect.width / 2) - rect.left - size / 2) + 'px';");
        sb.append("  wave.style.top = ((e.clientY || rect.top + rect.height / 2) - rect.top - size / 2) + 'px';");
        sb.append("  wave.className = 'ripple-wave';");
        sb.append("  btn.appendChild(wave);");
        sb.append("  setTimeout(() => wave.remove(), 620);");
        sb.append("}");

        sb.append("function launchFlyOrb(sourceEl) {");
        sb.append("  if (!sourceEl || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;");
        sb.append("  const targetEl = window.innerWidth <= 960 ? document.getElementById('mobile-cart-bar') : document.getElementById('cart-box');");
        sb.append("  if (!targetEl) return;");
        sb.append("  const sRect = sourceEl.getBoundingClientRect();");
        sb.append("  const tRect = targetEl.getBoundingClientRect();");
        sb.append("  const orb = document.createElement('div');");
        sb.append("  orb.className = 'fly-orb';");
        sb.append("  orb.textContent = '+1 phần';");
        sb.append("  orb.style.left = (sRect.left + sRect.width / 2 - 30) + 'px';");
        sb.append("  orb.style.top = (sRect.top + sRect.height / 2 - 14) + 'px';");
        sb.append("  document.body.appendChild(orb);");
        sb.append("  requestAnimationFrame(() => {");
        sb.append("    const dx = (tRect.left + tRect.width / 2) - (sRect.left + sRect.width / 2);");
        sb.append("    const dy = (tRect.top + 28) - (sRect.top + sRect.height / 2);");
        sb.append("    orb.style.transform = `translate3d(${dx}px, ${dy}px, 0) scale(0.55)`;");
        sb.append("    orb.style.opacity = '0.15';");
        sb.append("  });");
        sb.append("  setTimeout(() => orb.remove(), 640);");
        sb.append("}");

        sb.append("function filterCategory(cat, btn, ev) {");
        sb.append("  if (ev) spawnRipple(ev, btn);");
        sb.append("  document.querySelectorAll('.cat-pill').forEach(b => b.classList.remove('active'));");
        sb.append("  btn.classList.add('active');");
        sb.append("  btn.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });");
        sb.append("  let visibleIndex = 0;");
        sb.append("  document.querySelectorAll('.food-card').forEach(card => {");
        sb.append("    if (cat === 'all' || card.getAttribute('data-category') === cat) {");
        sb.append("      card.style.display = 'flex';");
        sb.append("      card.style.animation = 'none';");
        sb.append("      card.offsetHeight;");
        sb.append("      card.style.animation = `cardReveal 0.42s var(--ease-spring) ${visibleIndex * 40}ms backwards`;");
        sb.append("      visibleIndex++;");
        sb.append("    } else {");
        sb.append("      card.style.display = 'none';");
        sb.append("    }");
        sb.append("  });");
        sb.append("}");

        sb.append("function toggleMobileCart(open) {");
        sb.append("  const box = document.getElementById('cart-box');");
        sb.append("  const backdrop = document.getElementById('cart-backdrop');");
        sb.append("  if (open) {");
        sb.append("    box.classList.add('drawer-open');");
        sb.append("    backdrop.classList.add('open');");
        sb.append("  } else {");
        sb.append("    box.classList.remove('drawer-open');");
        sb.append("    backdrop.classList.remove('open');");
        sb.append("  }");
        sb.append("}");

        sb.append("function syncMobileBar() {");
        sb.append("  const header = document.querySelector('#cart-box .cart-header');");
        sb.append("  if (!header) return;");
        sb.append("  const qty = header.getAttribute('data-total-qty') || '0';");
        sb.append("  const price = header.getAttribute('data-total-price') || '0 đ';");
        sb.append("  document.getElementById('mobile-bar-qty').textContent = qty + ' phần trong giỏ';");
        sb.append("  document.getElementById('mobile-bar-price').textContent = price;");
        sb.append("}");

        sb.append("function showToast(msg) {");
        sb.append("  const t = document.getElementById('toast-popup');");
        sb.append("  t.innerHTML = `<span>${msg}</span><div class='toast-progress'></div>`;");
        sb.append("  t.classList.add('show');");
        sb.append("  clearTimeout(window.toastTimer);");
        sb.append("  window.toastTimer = setTimeout(() => { t.classList.remove('show'); }, 2300);");
        sb.append("}");

        sb.append("async function submitAjax(e, form) {");
        sb.append("  e.preventDefault();");
        sb.append("  const submitBtn = form.querySelector('button[type=submit]');");
        sb.append("  const bodyData = new URLSearchParams(new FormData(form));");
        sb.append("  try {");
        sb.append("    if (form.action.includes('add') && submitBtn) {");
        sb.append("      launchFlyOrb(submitBtn);");
        sb.append("      const oldText = submitBtn.textContent;");
        sb.append("      submitBtn.classList.add('added-state');");
        sb.append("      submitBtn.textContent = 'ĐÃ THÊM VÀO ĐƠN';");
        sb.append("      setTimeout(() => { submitBtn.classList.remove('added-state'); submitBtn.textContent = oldText; }, 900);");
        sb.append("    }");
        sb.append("    const res = await fetch(form.action, { method: 'POST', body: bodyData });");
        sb.append("    const html = await res.text();");
        sb.append("    const cartBox = document.getElementById('cart-box');");
        sb.append("    cartBox.innerHTML = html;");
        sb.append("    cartBox.classList.remove('cart-bump');");
        sb.append("    void cartBox.offsetWidth;");
        sb.append("    cartBox.classList.add('cart-bump');");
        sb.append("    syncMobileBar();");
        sb.append("    if (form.action.includes('add')) {");
        sb.append("      showToast('Đã thêm món gà vào phiếu đặt của bạn');");
        sb.append("      const noteInput = form.querySelector('input[name=note]');");
        sb.append("      if (noteInput) noteInput.value = '';");
        sb.append("    } else if (form.action.includes('submit')) {");
        sb.append("      showToast('Đơn đặt món đã được chuyển thẳng vào bếp');");
        sb.append("    }");
        sb.append("  } catch(err) { console.error(err); }");
        sb.append("}");
        sb.append("</script>");

        sb.append("</body></html>");
        return sb.toString();
    }

    public static String buildOwnerHtml(List<Order> orders, RestaurantOwner owner) {
        long revenue = RestaurantService.calculateTotalRevenue();
        long pendingOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long cookingOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.COOKING).count();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>Hệ Thống Điều Phối Bếp — Gà Vàng Phố Cổ</title>");
        sb.append("<link href='https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;600;700;800&display=swap' rel='stylesheet'>");
        sb.append("<style>");
        sb.append("* { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Be Vietnam Pro', sans-serif; }");
        sb.append("body { background: #0f172a; color: #f8fafc; padding: 25px; }");
        sb.append(".top-nav { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; padding-bottom:20px; border-bottom:1px solid #334155; margin-bottom:25px; }");
        sb.append(".logo { font-size:1.45em; font-weight:800; color:#fbbf24; letter-spacing:0.02em; }");
        sb.append(".metrics-grid { display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:20px; margin-bottom:30px; }");
        sb.append(".metric-tile { background: #1e293b; padding: 20px; border-radius: 14px; border: 1px solid #334155; }");
        sb.append(".metric-title { color: #94a3b8; font-size: 0.82em; text-transform: uppercase; font-weight: 700; letter-spacing: 0.05em; margin-bottom: 6px; }");
        sb.append(".metric-value { font-size: 2em; font-weight: 800; color: #f1f5f9; }");
        sb.append(".orders-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 20px; }");
        sb.append(".order-ticket { background: #1e293b; border-radius: 16px; padding: 20px; border: 1px solid #334155; display:flex; flex-direction:column; justify-content:space-between; }");
        sb.append(".ticket-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }");
        sb.append(".ticket-id { font-size: 1.2em; font-weight: 800; color: #fbbf24; }");
        sb.append(".status-tag { padding: 5px 12px; border-radius: 20px; font-size: 0.78em; font-weight: 700; color: white; }");
        sb.append(".customer-detail { background: #0f172a; padding: 12px; border-radius: 10px; margin-bottom: 14px; font-size: 0.9em; line-height: 1.5; }");
        sb.append(".food-items { margin: 12px 0; border-top: 1px dashed #334155; padding-top: 10px; flex-grow:1; }");
        sb.append(".food-line { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 0.95em; }");
        sb.append(".ticket-actions { margin-top: 15px; display: flex; gap: 8px; }");
        sb.append(".btn-cmd { flex: 1; padding: 10px; border: none; border-radius: 8px; font-weight: 700; cursor: pointer; color: white; font-size: 0.85em; transition: opacity 0.2s; }");
        sb.append(".btn-cmd:hover { opacity: 0.9; }");
        sb.append("</style></head><body>");

        // Nav
        sb.append("<div class='top-nav'>");
        sb.append("<div class='logo'>KITCHEN DISPLAY SYSTEM — GÀ VÀNG PHỐ CỔ</div>");
        sb.append("<div>Người trực: <strong>").append(owner.getName()).append("</strong> (").append(owner.getRoleName()).append(")</div>");
        sb.append("</div>");

        // Metrics
        sb.append("<div class='metrics-grid'>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Đơn Chờ Chế Biến</div><div class='metric-value' style='color:#f59e0b;'>").append(pendingOrders).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Đang Chế Biến</div><div class='metric-value' style='color:#38bdf8;'>").append(cookingOrders).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Tổng Số Đơn Nhận</div><div class='metric-value'>").append(orders.size()).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Doanh Thu Hoàn Tất</div><div class='metric-value' style='color:#4ade80;'>").append(MONEY.format(revenue)).append(" đ</div></div>");
        sb.append("</div>");

        // Tickets
        sb.append("<h3 style='margin-bottom:18px; color:#cbd5e1;'>Danh Sách Phiếu Order Thời Gian Thực</h3>");
        sb.append("<div class='orders-grid'>");
        if (orders.isEmpty()) {
            sb.append("<p style='color:#64748b; font-style:italic;'>Chưa có phiếu order nào từ khách hàng.</p>");
        } else {
            for (Order o : orders) {
                sb.append("<div class='order-ticket'>");
                sb.append("<div>");
                sb.append("<div class='ticket-header'>");
                sb.append("<div class='ticket-id'>PHIẾU #").append(o.getOrderId()).append("</div>");
                sb.append("<div class='status-tag' style='background:").append(o.getStatus().getColor()).append(";'>").append(o.getStatus().getTitle()).append("</div>");
                sb.append("</div>");

                sb.append("<div class='customer-detail'>");
                sb.append("Khách: <b>").append(o.getCustomer().getName()).append("</b> | Vị trí: <span style='color:#facc15; font-weight:bold;'>").append(o.getCustomer().getTableNumber()).append("</span><br>");
                sb.append("SĐT: ").append(o.getCustomer().getPhone()).append(" | Lúc: ").append(o.getCreatedAt());
                sb.append("</div>");

                sb.append("<div class='food-items'>");
                for (OrderItem oi : o.getItems()) {
                    sb.append("<div class='food-line'>");
                    sb.append("<span>• ").append(oi.getItem().getName()).append(" x<b style='color:#facc15;'>").append(oi.getQuantity()).append("</b></span>");
                    sb.append("<span>").append(MONEY.format(oi.getSubtotal())).append(" đ</span>");
                    sb.append("</div>");
                    if (!oi.getNote().isBlank()) {
                        sb.append("<div style='color:#fb7185; font-size:0.85em; margin:-4px 0 6px 12px;'>Ghi chú: ").append(oi.getNote()).append("</div>");
                    }
                }
                sb.append("</div>");
                sb.append("</div>");

                sb.append("<div>");
                sb.append("<div style='display:flex; justify-content:space-between; margin-top:10px; font-weight:700; border-top:1px solid #334155; padding-top:10px;'>");
                sb.append("<span>Tổng cộng:</span>");
                sb.append("<span style='color:#38bdf8; font-size:1.15em;'>").append(MONEY.format(o.calculateTotal())).append(" đ</span>");
                sb.append("</div>");

                sb.append("<div class='ticket-actions'>");
                if (o.getStatus() == OrderStatus.PENDING) {
                    sb.append(makeActionButton(o.getOrderId(), "COOKING", "Nhận chế biến", "#0284c7"));
                    sb.append(makeActionButton(o.getOrderId(), "CANCELLED", "Hủy đơn", "#dc2626"));
                } else if (o.getStatus() == OrderStatus.COOKING) {
                    sb.append(makeActionButton(o.getOrderId(), "READY", "Hoàn tất & Trả món", "#16a34a"));
                } else {
                    sb.append("<div style='color:#64748b; font-size:0.85em; text-align:center; width:100%; padding:8px 0;'>Đơn đã đóng lưu trữ</div>");
                }
                sb.append("</div></div>");
                sb.append("</div>");
            }
        }
        sb.append("</div>");

        // Quét đơn mới ngầm định kỳ
        sb.append("<script>");
        sb.append("let currentTotal = ").append(orders.size()).append(";");
        sb.append("setInterval(async () => {");
        sb.append("  try {");
        sb.append("    const res = await fetch('/');");
        sb.append("    const text = await res.text();");
        sb.append("    const parser = new DOMParser();");
        sb.append("    const doc = parser.parseFromString(text, 'text/html');");
        sb.append("    const newTotal = parseInt(doc.querySelector('.metric-tile:nth-child(3) .metric-value').innerText) || 0;");
        sb.append("    if (newTotal > currentTotal) {");
        sb.append("      new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3').play().catch(()=>{});");
        sb.append("      location.reload();");
        sb.append("    }");
        sb.append("  } catch(e) {}");
        sb.append("}, 2500);");
        sb.append("</script>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String makeActionButton(int id, String targetStatus, String label, String bg) {
        return "<form method='POST' action='/status' style='flex:1;'>"
                + "<input type='hidden' name='orderId' value='" + id + "'>"
                + "<input type='hidden' name='status' value='" + targetStatus + "'>"
                + "<button type='submit' class='btn-cmd' style='background:" + bg + "; width:100%;'>" + label + "</button>"
                + "</form>";
    }
}


// 4. MULTI-SERVER CONTROLLERS (KHÁCH HÀNG: 8080 | CHỦ QUÁN: 8081)


public class RestaurantSystem {
    private static final CartSession ACTIVE_CART = new CartSession();
    private static final RestaurantOwner MAIN_CHEF = new RestaurantOwner("chef_admin", "Bếp Trưởng Gà Vàng");

    public static void main(String[] args) throws IOException {
        int customerPort = 8080;
        int ownerPort = 8081;

        // SERVER KHÁCH HÀNG (PORT 8080)
        HttpServer customerServer = HttpServer.create(new InetSocketAddress(customerPort), 0);
        customerServer.createContext("/", exchange -> {
            String response = WebThemeBuilder.buildCustomerHtml(RestaurantService.getMenu(), ACTIVE_CART);
            sendText(exchange, response, 200);
        });

        customerServer.createContext("/cart/add", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> data = readFormData(exchange.getRequestBody());
                int id = Integer.parseInt(data.get("id"));
                String note = data.getOrDefault("note", "");
                RestaurantService.getMenu().stream()
                        .filter(m -> m.getId() == id)
                        .findFirst()
                        .ifPresent(m -> ACTIVE_CART.addProduct(m, note));

                String updatedCartHtml = WebThemeBuilder.buildCartInnerHtml(ACTIVE_CART, "");
                sendText(exchange, updatedCartHtml, 200);
            }
        });

        customerServer.createContext("/cart/quantity", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> data = readFormData(exchange.getRequestBody());
                int id = Integer.parseInt(data.get("id"));
                int delta = Integer.parseInt(data.get("delta"));
                ACTIVE_CART.changeQty(id, delta);

                String updatedCartHtml = WebThemeBuilder.buildCartInnerHtml(ACTIVE_CART, "");
                sendText(exchange, updatedCartHtml, 200);
            }
        });

        customerServer.createContext("/cart/submit", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> data = readFormData(exchange.getRequestBody());
                Customer customer = new Customer(
                        data.getOrDefault("name", "Khách bàn"),
                        data.getOrDefault("table", "Tại bàn"),
                        data.getOrDefault("phone", "")
                );
                Order created = RestaurantService.submitOrder(customer, ACTIVE_CART);
                String msg = (created != null)
                        ? "Đặt món thành công! Mã đơn #" + created.getOrderId() + " đã chuyển thẳng vào bếp."
                        : "Lỗi tạo đơn!";

                String updatedCartHtml = WebThemeBuilder.buildCartInnerHtml(ACTIVE_CART, msg);
                sendText(exchange, updatedCartHtml, 200);
            }
        });

        customerServer.setExecutor(null);
        customerServer.start();

        // SERVER CHỦ QUÁN (PORT 8081)
        HttpServer ownerServer = HttpServer.create(new InetSocketAddress(ownerPort), 0);
        ownerServer.createContext("/", exchange -> {
            String response = WebThemeBuilder.buildOwnerHtml(RestaurantService.getOrders(), MAIN_CHEF);
            sendText(exchange, response, 200);
        });

        ownerServer.createContext("/status", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> data = readFormData(exchange.getRequestBody());
                int orderId = Integer.parseInt(data.get("orderId"));
                OrderStatus newStatus = OrderStatus.valueOf(data.get("status"));
                RestaurantService.changeStatus(orderId, newStatus);
                redirect(exchange, "/");
            }
        });

        ownerServer.setExecutor(null);
        ownerServer.start();

        System.out.println("=================================================================");
        System.out.println("  HE THONG DAT MON & KDS — GA VANG PHO CO  ");
        System.out.println("  * CONG KHACH HANG:    http://localhost:" + customerPort);
        System.out.println("  * CONG CHU NHA HANG:  http://localhost:" + ownerPort);
        System.out.println("=================================================================");
    }

    private static void sendText(HttpExchange exchange, String content, int code) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void redirect(HttpExchange exchange, String dest) throws IOException {
        exchange.getResponseHeaders().set("Location", dest);
        exchange.sendResponseHeaders(303, -1);
    }

    private static Map<String, String> readFormData(InputStream input) throws IOException {
        Map<String, String> res = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String body = reader.readLine();
        if (body == null || body.isBlank()) return res;

        for (String pair : body.split("&")) {
            String[] tokens = pair.split("=");
            String key = URLDecoder.decode(tokens[0], StandardCharsets.UTF_8);
            String val = tokens.length > 1 ? URLDecoder.decode(tokens[1], StandardCharsets.UTF_8) : "";
            res.put(key, val);
        }
        return res;
    }
}
