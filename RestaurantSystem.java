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
    PENDING("Chờ chuẩn bị", "#ff9800"),
    COOKING("Bếp đang nấu", "#0288d1"),
    READY("Đã xong món", "#2e7d32"),
    CANCELLED("Đã hủy", "#d32f2f");

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

// 2. SERVICE & DATA REPOSITORY

class RestaurantService {
    private static final List<MenuItem> MENU = new ArrayList<>();
    private static final List<Order> ORDERS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(501);

    static {
        MENU.add(new MenuItem(1, "Gà Rán Giòn Vui Vẻ", 35000, "ga-ran", "https://i.postimg.cc/wMyGb9mC/hinh-anh-ga-ran.jpg", "Lớp vỏ ngoài giòn rụm màu vàng óng, thịt mọng nước"));
        MENU.add(new MenuItem(2, "Mì Ý Sốt Bò Bằm", 38000, "ga-ran", "https://i.postimg.cc/BbLVwJFQ/mi-xao.jpg", "Sốt cà chua bò bằm phô mai đậm đà theo vị ngọt đặc trưng"));
        MENU.add(new MenuItem(3, "Cơm Gà Xối Mỡ", 42000, "com-pho", "https://i.postimg.cc/Rh6DbvtZ/com-ga-xoi-mo.jpg", "Cơm đảo vàng óng kèm đùi gà xối mỡ giòn tan"));
        MENU.add(new MenuItem(4, "Phở Bò Thượng Hạng", 45000, "com-pho", "https://i.postimg.cc/FzdBnhSK/pho-bo.jpg", "Nước dùng hầm xương thảo mộc thơm lừng, thịt bò tươi mềm"));
        MENU.add(new MenuItem(5, "Bún Chả Nướng Than", 35000, "com-pho", "https://i.postimg.cc/NF9zPQXG/bun-cha.webp", "Chả viên nướng xém cạnh thơm phức, nước mắm tỏi ớt"));
        MENU.add(new MenuItem(6, "Cơm Tấm Sườn Nướng", 42000, "com-pho", "https://i.postimg.cc/j2wFkRfx/com-tam.jpg", "Sườn cốt lết ướp đậm vị nướng than hồng ăn kèm mỡ hành"));
        MENU.add(new MenuItem(7, "Bánh Mì Kẹp Thịt Giòn", 28000, "com-pho", "https://i.postimg.cc/kG6ph7Sn/banh-mi.jpg", "Vỏ bánh giòn rụm với pate thơm béo, chả lụa và dưa góp"));
        MENU.add(new MenuItem(8, "Khoai Tây Lắc Phô Mai", 25000, "an-vat", "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500&h=400&fit=crop", "Khoai tây chiên giòn tan ngập trong bột phô mai béo ngậy"));
        MENU.add(new MenuItem(9, "Khoai Lang Kén", 25000, "an-vat", "https://i.postimg.cc/prh61PF3/dia-khoai-lang-ken.jpg", "Vị ngọt bùi của khoai hòa cùng mùi thơm của mè đen"));
        MENU.add(new MenuItem(10, "Trứng Cút Chiên Giòn", 20000, "an-vat", "https://i.postimg.cc/gjXTBGhF/trung-cut-chien.jpg", "Món ăn vặt thơm bùi, chấm kèm sốt tương ớt đậm vị"));
        MENU.add(new MenuItem(11, "Trà Sữa Trân Châu Full", 30000, "do-uong", "https://i.postimg.cc/2ybKXCWC/tra-sua.png", "Vị trà đậm đà, sữa béo ngậy kèm trân châu đen giòn dai"));
        MENU.add(new MenuItem(12, "Cà Phê Muối Đặc Biệt", 25000, "do-uong", "https://i.postimg.cc/4y7SFXcZ/ca-phe-muoi.jpg", "Cà phê đậm đà kết hợp lớp kem mặn béo ngậy"));
        MENU.add(new MenuItem(13, "Sinh Tố Dâu Tây Tươi", 32000, "do-uong", "https://i.postimg.cc/PJpRFdDH/sinh-to-dau-tay.png", "Dâu tây tươi ngọt dịu, đá xay thanh mát giải nhiệt"));
        MENU.add(new MenuItem(14, "Trà Chanh Thái Xanh", 18000, "do-uong", "https://i.postimg.cc/BbLVwJFW/tra-chanh-thai-xanh.jpg", "Hương chanh chua thanh mát, thơm nồng trà xanh tươi"));
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


// 3. VIEW BUILDER: UI (KHÁCH HÀNG) & UI QUẢN TRỊ (CHỦ QUÁN)


class WebThemeBuilder {
    private static final NumberFormat MONEY = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    public static String buildCartInnerHtml(CartSession cart, String notification) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='cart-title'>");
        sb.append("<span>🛒 Giỏ Của Bạn</span>");
        sb.append("<span style='background:var(--jb-red); color:white; font-size:0.65em; padding:4px 12px; border-radius:20px;'>")
                .append(cart.getTotalQuantity()).append(" phần</span>");
        sb.append("</div>");

        if (notification != null && !notification.isBlank()) {
            sb.append("<div class='toast'>").append(notification).append("</div>");
        }

        if (cart.getItems().isEmpty()) {
            sb.append("<div style='text-align:center; padding:35px 10px; color:#999;'>");
            sb.append("<div style='font-size:3em; margin-bottom:10px;'>🍗</div>");
            sb.append("Chưa có món nào. Hãy chọn món ngon yêu thích nhé!");
            sb.append("</div>");
        } else {
            for (OrderItem order : cart.getItems()) {
                sb.append("<div class='cart-item'>");
                sb.append("<div style='flex:1;'>");
                sb.append("<strong style='color:#222;'>").append(order.getItem().getName()).append("</strong><br>");
                sb.append("<span style='color:var(--jb-red); font-weight:700;'>").append(MONEY.format(order.getItem().getPrice())).append(" đ</span>");
                if (!order.getNote().isBlank()) {
                    sb.append("<div style='font-size:0.8em; color:#d6001c; font-style:italic;'>📝 ").append(order.getNote()).append("</div>");
                }
                sb.append("</div>");

                sb.append("<div style='display:flex; align-items:center; gap:8px;'>");
                sb.append("<form onsubmit='submitAjax(event, this)' action='/cart/quantity' style='display:inline;'>");
                sb.append("<input type='hidden' name='id' value='").append(order.getItem().getId()).append("'>");
                sb.append("<input type='hidden' name='delta' value='-1'>");
                sb.append("<button type='submit' class='btn-qty'>-</button>");
                sb.append("</form>");

                sb.append("<strong style='min-width:20px; text-align:center;'>").append(order.getQuantity()).append("</strong>");

                sb.append("<form onsubmit='submitAjax(event, this)' action='/cart/quantity' style='display:inline;'>");
                sb.append("<input type='hidden' name='id' value='").append(order.getItem().getId()).append("'>");
                sb.append("<input type='hidden' name='delta' value='1'>");
                sb.append("<button type='submit' class='btn-qty'>+</button>");
                sb.append("</form>");
                sb.append("</div></div>");
            }

            sb.append("<div style='display:flex; justify-content:space-between; margin:20px 0; font-size:1.25em;'>");
            sb.append("<strong>Tổng hóa đơn:</strong>");
            sb.append("<strong style='color:var(--jb-red);'>").append(MONEY.format(cart.getTotalPrice())).append(" đ</strong>");
            sb.append("</div>");

            sb.append("<form onsubmit='submitAjax(event, this)' action='/cart/submit'>");
            sb.append("<input type='text' name='name' class='input-control' required placeholder='Tên người nhận (VD: Lan Anh)'>");
            sb.append("<input type='text' name='table' class='input-control' required placeholder='Bàn số (hoặc Mang về)'>");
            sb.append("<input type='text' name='phone' class='input-control' placeholder='Số điện thoại'>");
            sb.append("<button type='submit' class='btn-checkout'>ĐẶT NGAY BÂY GIỜ</button>");
            sb.append("</form>");
        }
        return sb.toString();
    }

    public static String buildCustomerHtml(List<MenuItem> menu, CartSession cart) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>Nhà Hàng PTIT - Đặt Món Trực Tuyến</title>");
        sb.append("<link href='https://fonts.googleapis.com/css2?family=Signika:wght@500;700&family=Quicksand:wght@500;600;700&display=swap' rel='stylesheet'>");
        sb.append("<style>");
        sb.append(":root { --jb-red: #d6001c; --jb-red-dark: #b30016; --jb-yellow: #ffc72c; --jb-yellow-hover: #e0aa1b; --bg: #fffbf5; }");
        sb.append("* { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Quicksand', sans-serif; }");
        sb.append("body { background: var(--bg); color: #2c3e50; }");
        sb.append("header { background: var(--jb-red); color: white; padding: 22px 20px; box-shadow: 0 4px 15px rgba(214,0,28,0.25); text-align: center; }");
        sb.append(".brand-title { font-family: 'Signika', sans-serif; font-size: 2.3em; letter-spacing: 1px; color: var(--jb-yellow); text-transform: uppercase; margin-bottom: 4px; text-shadow: 2px 2px 0px rgba(0,0,0,0.2); }");
        sb.append(".brand-sub { font-size: 1.05em; font-weight: 600; opacity: 0.95; }");
        sb.append(".category-bar { display: flex; justify-content: center; gap: 12px; margin: 25px auto; padding: 0 15px; flex-wrap: wrap; }");
        sb.append(".cat-pill { padding: 10px 22px; border-radius: 30px; font-weight: 700; color: #444; background: white; border: 2px solid #f0e6da; cursor: pointer; transition: 0.25s all ease; box-shadow: 0 2px 6px rgba(0,0,0,0.04); font-size: 0.95em; outline: none; }");
        sb.append(".cat-pill.active, .cat-pill:hover { background: var(--jb-yellow); color: var(--jb-red-dark); border-color: var(--jb-yellow); transform: translateY(-2px); box-shadow: 0 5px 12px rgba(255,199,44,0.4); }");
        sb.append(".main-layout { display: flex; max-width: 1300px; margin: 0 auto; gap: 30px; padding: 0 20px 40px; }");
        sb.append(".menu-grid { flex: 1; display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 24px; align-items: start; }");
        sb.append(".food-card { background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 6px 18px rgba(214,0,28,0.08); border: 2px solid #fff0e6; display: flex; flex-direction: column; justify-content: space-between; transition: 0.25s all ease; }");
        sb.append(".food-card:hover { transform: translateY(-6px); box-shadow: 0 12px 25px rgba(214,0,28,0.15); border-color: var(--jb-yellow); }");
        sb.append(".food-img { width: 100%; height: 175px; object-fit: cover; background: #eee; }");
        sb.append(".food-info { padding: 16px; flex-grow: 1; display: flex; flex-direction: column; justify-content: space-between; }");
        sb.append(".food-title { font-family: 'Signika', sans-serif; font-size: 1.25em; color: #222; margin-bottom: 6px; }");
        sb.append(".food-desc { font-size: 0.85em; color: #777; line-height: 1.4; margin-bottom: 12px; height: 36px; overflow: hidden; }");
        sb.append(".food-price { font-size: 1.3em; font-weight: 700; color: var(--jb-red); margin-bottom: 12px; }");
        sb.append(".food-note { width: 100%; padding: 8px 12px; border: 1.5px solid #eee; border-radius: 10px; font-size: 0.85em; margin-bottom: 10px; outline: none; transition: 0.2s; background: #fafafa; }");
        sb.append(".food-note:focus { border-color: var(--jb-red); background: white; }");
        sb.append(".btn-add-cart { width: 100%; background: var(--jb-red); color: white; border: none; padding: 11px; border-radius: 12px; font-weight: 700; cursor: pointer; transition: 0.2s; font-size: 0.95em; box-shadow: 0 4px 10px rgba(214,0,28,0.2); }");
        sb.append(".btn-add-cart:hover { background: var(--jb-red-dark); }");
        sb.append(".cart-container { width: 380px; background: white; border-radius: 20px; padding: 24px; box-shadow: 0 8px 25px rgba(0,0,0,0.08); border: 2px solid #fae8d8; height: fit-content; position: sticky; top: 20px; }");
        sb.append(".cart-title { font-family: 'Signika', sans-serif; font-size: 1.5em; color: var(--jb-red); display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; border-bottom: 2px dashed #f0e6da; padding-bottom: 12px; }");
        sb.append(".cart-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f6f0ea; }");
        sb.append(".btn-qty { width: 28px; height: 28px; border-radius: 50%; border: 2px solid var(--jb-red); background: white; color: var(--jb-red); font-weight: bold; cursor: pointer; transition: 0.2s; }");
        sb.append(".btn-qty:hover { background: var(--jb-red); color: white; }");
        sb.append(".btn-checkout { width: 100%; background: var(--jb-yellow); color: var(--jb-red-dark); font-family: 'Signika', sans-serif; font-size: 1.25em; padding: 15px; border: none; border-radius: 14px; font-weight: bold; cursor: pointer; box-shadow: 0 5px 15px rgba(255,199,44,0.4); text-transform: uppercase; transition: 0.25s; }");
        sb.append(".btn-checkout:hover { background: var(--jb-yellow-hover); transform: translateY(-2px); }");
        sb.append(".input-control { width: 100%; padding: 10px 14px; border: 1.5px solid #e0e0e0; border-radius: 10px; margin-bottom: 12px; font-size: 0.9em; outline: none; }");
        sb.append(".toast { background: #d4edda; color: #155724; padding: 12px 18px; border-radius: 12px; margin-bottom: 15px; font-weight: bold; border-left: 5px solid #28a745; animation: popIn 0.3s ease; }");
        sb.append("#toast-popup { position: fixed; bottom: 30px; right: 30px; background: #2c3e50; color: white; padding: 14px 22px; border-radius: 12px; box-shadow: 0 8px 25px rgba(0,0,0,0.25); display: none; z-index: 1000; font-weight: 600; border-left: 5px solid var(--jb-yellow); }");
        sb.append("@keyframes popIn { from { transform: scale(0.9); opacity: 0; } to { transform: scale(1); opacity: 1; } }");
        sb.append("@media (max-width: 960px) { .main-layout { flex-direction: column; } .cart-container { width: 100%; position: static; } }");
        sb.append("</style></head><body>");

        // Header
        sb.append("<header>");
        sb.append("<div class='brand-title'>🎓 NHÀ HÀNG PTIT 🍜</div>");
        sb.append("<p class='brand-sub'>Học hết mình - Ăn nhiệt tình</p>");
        sb.append("</header>");

        // Thanh danh mục dùng thẻ button & gọi hàm JavaScript (Không load trang)
        sb.append("<div class='category-bar'>");
        sb.append("<button type='button' class='cat-pill active' onclick=\"filterCategory('all', this)\">Tất cả món</button>");
        sb.append("<button type='button' class='cat-pill' onclick=\"filterCategory('ga-ran', this)\">Gà Giòn & Mì Ý</button>");
        sb.append("<button type='button' class='cat-pill' onclick=\"filterCategory('com-pho', this)\">Món No Đặc Biệt</button>");
        sb.append("<button type='button' class='cat-pill' onclick=\"filterCategory('an-vat', this)\">Ăn Kèm Giòn Rụm</button>");
        sb.append("<button type='button' class='cat-pill' onclick=\"filterCategory('do-uong', this)\">Giải Khát Mát Lạnh</button>");
        sb.append("</div>");

        sb.append("<div class='main-layout'>");

        // Grid Menu hiển thị tất cả các món ăn kèm data-category
        sb.append("<div class='menu-grid'>");
        for (MenuItem item : menu) {
            sb.append("<div class='food-card' data-category='").append(item.getCategory()).append("'>");
            sb.append("<img class='food-img' src='").append(item.getImageUrl()).append("' alt='").append(item.getName()).append("' onerror=\"this.src='https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&h=400&fit=crop';\">");
            sb.append("<div class='food-info'>");
            sb.append("<div>");
            sb.append("<div class='food-title'>").append(item.getName()).append("</div>");
            sb.append("<div class='food-desc'>").append(item.getDescription()).append("</div>");
            sb.append("<div class='food-price'>").append(MONEY.format(item.getPrice())).append(" đ</div>");
            sb.append("</div>");

            sb.append("<form onsubmit='submitAjax(event, this)' action='/cart/add'>");
            sb.append("<input type='hidden' name='id' value='").append(item.getId()).append("'>");
            sb.append("<input type='text' name='note' class='food-note' placeholder='Ghi chú món ăn...'>");
            sb.append("<button type='submit' class='btn-add-cart'>+ THÊM VÀO GIỎ</button>");
            sb.append("</form>");
            sb.append("</div></div>");
        }
        sb.append("</div>");

        // Giỏ hàng cập nhật qua AJAX
        sb.append("<div id='cart-box' class='cart-container'>");
        sb.append(buildCartInnerHtml(cart, ""));
        sb.append("</div>");

        sb.append("</div>");

        // Toast thông báo
        sb.append("<div id='toast-popup'></div>");

        // Script xử lý chuyển tab và gửi đơn hàng ngầm 100%
        sb.append("<script>");
        // Hàm lọc danh mục không tải lại trang
        sb.append("function filterCategory(cat, btn) {");
        sb.append("  document.querySelectorAll('.cat-pill').forEach(b => b.classList.remove('active'));");
        sb.append("  btn.classList.add('active');");
        sb.append("  document.querySelectorAll('.food-card').forEach(card => {");
        sb.append("    if (cat === 'all' || card.getAttribute('data-category') === cat) {");
        sb.append("      card.style.display = 'flex';");
        sb.append("    } else {");
        sb.append("      card.style.display = 'none';");
        sb.append("    }");
        sb.append("  });");
        sb.append("}");

        sb.append("function showToast(msg) {");
        sb.append("  const t = document.getElementById('toast-popup');");
        sb.append("  t.innerText = msg;");
        sb.append("  t.style.display = 'block';");
        sb.append("  clearTimeout(window.toastTimer);");
        sb.append("  window.toastTimer = setTimeout(() => { t.style.display = 'none'; }, 2200);");
        sb.append("}");

        sb.append("async function submitAjax(e, form) {");
        sb.append("  e.preventDefault();");
        sb.append("  const bodyData = new URLSearchParams(new FormData(form));");
        sb.append("  try {");
        sb.append("    const res = await fetch(form.action, { method: 'POST', body: bodyData });");
        sb.append("    const html = await res.text();");
        sb.append("    document.getElementById('cart-box').innerHTML = html;");
        sb.append("    if(form.action.includes('add')) {");
        sb.append("      showToast('✨ Đã thêm món vào giỏ hàng!');");
        sb.append("      const noteInput = form.querySelector('input[name=note]');");
        sb.append("      if(noteInput) noteInput.value = '';");
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
        sb.append("<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'><title>Hệ Thống Quản Lý Bếp & Đơn Hàng</title>");
        sb.append("<link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap' rel='stylesheet'>");
        sb.append("<style>");
        sb.append("* { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }");
        sb.append("body { background: #0f172a; color: #f8fafc; padding: 25px; }");
        sb.append(".top-nav { display:flex; justify-content:space-between; align-items:center; padding-bottom:20px; border-bottom:1px solid #334155; margin-bottom:25px; }");
        sb.append(".logo { font-size:1.6em; font-weight:700; color:#38bdf8; display:flex; align-items:center; gap:8px; }");
        sb.append(".metrics-grid { display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:20px; margin-bottom:30px; }");
        sb.append(".metric-tile { background: #1e293b; padding: 20px; border-radius: 14px; border: 1px solid #334155; }");
        sb.append(".metric-title { color: #94a3b8; font-size: 0.85em; text-transform: uppercase; font-weight: 600; margin-bottom: 6px; }");
        sb.append(".metric-value { font-size: 2em; font-weight: 700; color: #f1f5f9; }");
        sb.append(".orders-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 20px; }");
        sb.append(".order-ticket { background: #1e293b; border-radius: 16px; padding: 20px; border: 1px solid #334155; display:flex; flex-direction:column; justify-content:space-between; }");
        sb.append(".ticket-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }");
        sb.append(".ticket-id { font-size: 1.25em; font-weight: 700; color: #38bdf8; }");
        sb.append(".status-tag { padding: 5px 12px; border-radius: 20px; font-size: 0.8em; font-weight: 700; color: white; }");
        sb.append(".customer-detail { background: #0f172a; padding: 12px; border-radius: 10px; margin-bottom: 14px; font-size: 0.9em; }");
        sb.append(".food-items { margin: 12px 0; border-top: 1px dashed #334155; padding-top: 10px; flex-grow:1; }");
        sb.append(".food-line { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 0.95em; }");
        sb.append(".ticket-actions { margin-top: 15px; display: flex; gap: 8px; }");
        sb.append(".btn-cmd { flex: 1; padding: 10px; border: none; border-radius: 8px; font-weight: 700; cursor: pointer; color: white; font-size: 0.85em; }");
        sb.append("</style></head><body>");

        // Nav
        sb.append("<div class='top-nav'>");
        sb.append("<div class='logo'><span>👨‍🍳</span> KITCHEN DISPLAY SYSTEM (KDS)</div>");
        sb.append("<div>Người trực: <strong>").append(owner.getName()).append("</strong> (").append(owner.getRoleName()).append(")</div>");
        sb.append("</div>");

        // Metrics
        sb.append("<div class='metrics-grid'>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Đơn Chờ Nấu</div><div class='metric-value' style='color:#ff9800;'>").append(pendingOrders).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Đang Trên Bếp</div><div class='metric-value' style='color:#0288d1;'>").append(cookingOrders).append("</div></div>");
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
                sb.append("<div class='ticket-id'>TICKET #").append(o.getOrderId()).append("</div>");
                sb.append("<div class='status-tag' style='background:").append(o.getStatus().getColor()).append(";'>").append(o.getStatus().getTitle()).append("</div>");
                sb.append("</div>");

                sb.append("<div class='customer-detail'>");
                sb.append("👤 <b>").append(o.getCustomer().getName()).append("</b> | Vị trí: <span style='color:#facc15; font-weight:bold;'>").append(o.getCustomer().getTableNumber()).append("</span><br>");
                sb.append("📞 ").append(o.getCustomer().getPhone()).append(" | 🕒 ").append(o.getCreatedAt());
                sb.append("</div>");

                sb.append("<div class='food-items'>");
                for (OrderItem oi : o.getItems()) {
                    sb.append("<div class='food-line'>");
                    sb.append("<span>• ").append(oi.getItem().getName()).append(" x<b style='color:#facc15;'>").append(oi.getQuantity()).append("</b></span>");
                    sb.append("<span>").append(MONEY.format(oi.getSubtotal())).append(" đ</span>");
                    sb.append("</div>");
                    if (!oi.getNote().isBlank()) {
                        sb.append("<div style='color:#fb7185; font-size:0.85em; margin:-4px 0 6px 12px;'>➥ Ghi chú: ").append(oi.getNote()).append("</div>");
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
                    sb.append(makeActionButton(o.getOrderId(), "COOKING", "Bếp nấu", "#0288d1"));
                    sb.append(makeActionButton(o.getOrderId(), "CANCELLED", "Hủy", "#ef4444"));
                } else if (o.getStatus() == OrderStatus.COOKING) {
                    sb.append(makeActionButton(o.getOrderId(), "READY", "Trả món", "#16a34a"));
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
    private static final RestaurantOwner MAIN_CHEF = new RestaurantOwner("chef_admin", "Bếp Trưởng PTIT");

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
                        ? "🎉 Đặt món thành công! Mã đơn #" + created.getOrderId() + " đã gửi thẳng vào bếp."
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
        System.out.println("  HE THONG DAT MON & KDS NHA HANG  ");
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