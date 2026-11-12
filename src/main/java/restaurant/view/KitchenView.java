package restaurant.view;

import java.util.List;
import restaurant.model.Order;
import restaurant.model.OrderItem;
import restaurant.model.OrderStatus;
import restaurant.model.User;

public final class KitchenView {
    private KitchenView() {
    }

    public static String buildOwnerHtml(List<Order> orders, User owner, long revenue) {
        long pendingOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long cookingOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.COOKING).count();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>Hệ Thống Điều Phối Bếp — Gà Vàng Phố Cổ</title>");
        sb.append("<link href='https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;600;700;800&display=swap' rel='stylesheet'>");
        sb.append("<link rel='stylesheet' href='/assets/kitchen.css'>");
        sb.append("<script src='/assets/kitchen.js' defer></script></head><body>");

        // Nav
        sb.append("<div class='top-nav'>");
        sb.append("<div class='logo'>KITCHEN DISPLAY SYSTEM — GÀ VÀNG PHỐ CỔ</div>");
        sb.append("<div>Người trực: <strong>").append(Html.escape(owner.getName())).append("</strong> (").append(Html.escape(owner.getRoleName())).append(")</div>");
        sb.append("</div>");

        // Metrics
        sb.append("<div class='metrics-grid'>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Đơn Chờ Chế Biến</div><div class='metric-value' style='color:#f59e0b;'>").append(pendingOrders).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Đang Chế Biến</div><div class='metric-value' style='color:#38bdf8;'>").append(cookingOrders).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Tổng Số Đơn Nhận</div><div class='metric-value'>").append(orders.size()).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Doanh Thu Hoàn Tất</div><div class='metric-value' style='color:#4ade80;'>").append(Html.money(revenue)).append(" đ</div></div>");
        sb.append("</div>");

        // Tickets
        sb.append("<h3 style='margin-bottom:18px; color:#cbd5e1;'>Danh Sách Phiếu Order Thời Gian Thực</h3>");
        sb.append("<div class='orders-grid'>");
        if (orders.isEmpty()) {
            sb.append("<p style='color:#64748b; font-style:italic;'>Chưa có phiếu order nào từ khách hàng.</p>");
        } else {
            for (Order o : orders) {
                OrderStatus status = o.getStatus();
                sb.append("<div class='order-ticket'>");
                sb.append("<div>");
                sb.append("<div class='ticket-header'>");
                sb.append("<div class='ticket-id'>PHIẾU #").append(o.getOrderId()).append("</div>");
                sb.append("<div class='status-tag' style='background:").append(status.getColor()).append(";'>").append(status.getTitle()).append("</div>");
                sb.append("</div>");

                sb.append("<div class='customer-detail'>");
                sb.append("Khách: <b>").append(Html.escape(o.getCustomer().getName())).append("</b> | Vị trí: <span style='color:#facc15; font-weight:bold;'>").append(Html.escape(o.getCustomer().getTableNumber())).append("</span><br>");
                sb.append("SĐT: ").append(Html.escape(o.getCustomer().getPhone().isEmpty() ? "Chưa có SĐT" : o.getCustomer().getPhone())).append(" | Lúc: ").append(o.getCreatedAt());
                sb.append("</div>");

                sb.append("<div class='food-items'>");
                for (OrderItem oi : o.getItems()) {
                    sb.append("<div class='food-line'>");
                    sb.append("<span>• ").append(Html.escape(oi.getItem().getName())).append(" x<b style='color:#facc15;'>").append(oi.getQuantity()).append("</b></span>");
                    sb.append("<span>").append(Html.money(oi.getSubtotal())).append(" đ</span>");
                    sb.append("</div>");
                    if (!oi.getNote().isBlank()) {
                        sb.append("<div style='color:#fb7185; font-size:0.85em; margin:-4px 0 6px 12px;'>Ghi chú: ").append(Html.escape(oi.getNote())).append("</div>");
                    }
                }
                sb.append("</div>");
                sb.append("</div>");

                sb.append("<div>");
                sb.append("<div style='display:flex; justify-content:space-between; margin-top:10px; font-weight:700; border-top:1px solid #334155; padding-top:10px;'>");
                sb.append("<span>Tổng cộng:</span>");
                sb.append("<span style='color:#38bdf8; font-size:1.15em;'>").append(Html.money(o.calculateTotal())).append(" đ</span>");
                sb.append("</div>");

                sb.append("<div class='ticket-actions'>");
                if (status == OrderStatus.PENDING) {
                    sb.append(makeActionButton(o.getOrderId(), OrderStatus.COOKING, "Nhận chế biến", "#0284c7"));
                    sb.append(makeActionButton(o.getOrderId(), OrderStatus.CANCELLED, "Hủy đơn", "#dc2626"));
                } else if (status == OrderStatus.COOKING) {
                    sb.append(makeActionButton(o.getOrderId(), OrderStatus.READY, "Hoàn tất & Trả món", "#16a34a"));
                } else {
                    sb.append("<div style='color:#64748b; font-size:0.85em; text-align:center; width:100%; padding:8px 0;'>Đơn đã đóng lưu trữ</div>");
                }
                sb.append("</div></div>");
                sb.append("</div>");
            }
        }
        sb.append("</div>");



        sb.append("</body></html>");
        return sb.toString();
    }

    private static String makeActionButton(int id, OrderStatus targetStatus, String label, String bg) {
        return "<form method='POST' action='/status' style='flex:1;'>"
                + "<input type='hidden' name='orderId' value='" + id + "'>"
                + "<input type='hidden' name='status' value='" + targetStatus.name() + "'>"
                + "<button type='submit' class='btn-cmd' style='background:" + bg + "; width:100%;'>" + label + "</button>"
                + "</form>";
    }
}
