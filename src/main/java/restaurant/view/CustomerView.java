package restaurant.view;

import java.util.List;
import restaurant.model.CartSession;
import restaurant.model.MenuItem;
import restaurant.model.OrderItem;

public final class CustomerView {
    private CustomerView() {
    }

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
        List<OrderItem> items = cart.getItems();
        int totalQty = items.stream().mapToInt(OrderItem::getQuantity).sum();
        long totalPrice = items.stream().mapToLong(OrderItem::getSubtotal).sum();

        sb.append("<div class='cart-header' data-total-qty='").append(totalQty)
          .append("' data-total-price='").append(Html.money(totalPrice)).append(" đ'>");
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
            sb.append("<div class='order-banner'>").append(Html.escape(notification)).append("</div>");
        }

        if (items.isEmpty()) {
            sb.append("<div class='cart-empty'>");
            sb.append("<div class='empty-halo'></div>");
            sb.append("<p class='empty-title'>Chưa có món nào được chọn</p>");
            sb.append("<p class='empty-sub'>Chạm vào món gà hoặc cơm gà bất kỳ bên trái để bắt đầu lên đơn.</p>");
            sb.append("</div>");
        } else {
            sb.append("<div class='cart-items-list'>");
            for (OrderItem order : items) {
                sb.append("<div class='cart-item'>");
                sb.append("<div class='cart-item-info'>");
                sb.append("<div class='cart-item-name'>").append(Html.escape(order.getItem().getName())).append("</div>");
                sb.append("<div class='cart-item-price'>").append(Html.money(order.getItem().getPrice())).append(" đ</div>");
                if (!order.getNote().isBlank()) {
                    sb.append("<div class='cart-item-note'>Ghi chú: ").append(Html.escape(order.getNote())).append("</div>");
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
            sb.append("<strong class='summary-total'>").append(Html.money(totalPrice)).append(" đ</strong>");
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
            sb.append("<input type='tel' name='phone' class='input-control' placeholder='Không bắt buộc. VD: 0912345678'>");
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
        sb.append("<link rel='stylesheet' href='/assets/customer.css'>");
        sb.append("<script src='/assets/customer.js' defer></script></head><body>");

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
            sb.append("<article class='food-card' data-category='").append(Html.escape(item.getCategory()))
              .append("' style='animation-delay: ").append(idx * 45).append("ms;' onmousemove='handleCardSpotlight(event, this)'>");
            sb.append("<div class='food-media'>");
            sb.append("<span class='food-cat-tag'>").append(Html.escape(getCategoryLabel(item.getCategory()))).append("</span>");
            sb.append("<img class='food-img' src='").append(Html.escape(item.getImageUrl())).append("' alt='").append(Html.escape(item.getName()))
              .append("' loading='lazy' onerror=\"this.src='https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=600&h=420&fit=crop';\">");
            sb.append("<span class='food-price-badge'>").append(Html.money(item.getPrice())).append(" đ</span>");
            sb.append("</div>");

            sb.append("<div class='food-info'>");
            sb.append("<div>");
            sb.append("<h3 class='food-title'>").append(Html.escape(item.getName())).append("</h3>");
            sb.append("<p class='food-desc'>").append(Html.escape(item.getDescription())).append("</p>");
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
        sb.append("<span id='mobile-bar-price' class='mobile-bar-price'>").append(Html.money(cart.getTotalPrice())).append(" đ</span>");
        sb.append("</div>");
        sb.append("<span class='mobile-bar-cta'>Xem Giỏ & Đặt Món</span>");
        sb.append("</div>");

        // Toast Notification
        sb.append("<div id='toast-popup' role='status' aria-live='polite'></div>");



        sb.append("</body></html>");
        return sb.toString();
    }
}
