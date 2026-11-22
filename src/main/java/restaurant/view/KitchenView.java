package restaurant.view;

import java.util.List;
import restaurant.model.Customer;
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
        sb.append("<link rel='icon' href='data:,'>");
        sb.append("<link href='https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;600;700;800&display=swap' rel='stylesheet'>");
        sb.append("<link rel='stylesheet' href='/assets/kitchen.css'>");
        sb.append("<script src='/assets/kitchen.js' defer></script></head><body>");

        // Nav
        sb.append("<div class='top-nav'>");
        sb.append("<div class='logo'>KITCHEN DISPLAY SYSTEM — GÀ VÀNG PHỐ CỔ</div>");
        sb.append("<div>Người trực: <strong>").append(Html.escape(owner.getProfileSummary())).append("</strong></div>");
        sb.append("</div>");

        // Metrics
        sb.append("<div class='metrics-grid'>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Đơn Chờ Chế Biến</div><div class='metric-value metric-pending'>").append(pendingOrders).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Đang Chế Biến</div><div class='metric-value metric-cooking'>").append(cookingOrders).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Tổng Số Đơn Nhận</div><div class='metric-value'>").append(orders.size()).append("</div></div>");
        sb.append("<div class='metric-tile'><div class='metric-title'>Doanh Thu Hoàn Tất</div><div class='metric-value metric-revenue'>").append(Html.money(revenue)).append(" đ</div></div>");
        sb.append("</div>");

        // Tickets
        sb.append("<h3 class='section-heading'>Danh Sách Phiếu Order Thời Gian Thực</h3>");
        sb.append("<div class='orders-grid'>");
        if (orders.isEmpty()) {
            sb.append("<p class='empty-orders'>Chưa có phiếu order nào từ khách hàng.</p>");
        } else {
            for (Order o : orders) {
                OrderStatus status = o.getStatus();
                Customer customer = o.getCustomer();
                sb.append("<div class='order-ticket'>");
                sb.append("<div>");
                sb.append("<div class='ticket-header'>");
                sb.append("<div class='ticket-id'>PHIẾU #").append(o.getOrderId()).append("</div>");
                sb.append("<div class='status-tag ").append(statusClass(status)).append("'>")
                  .append(Html.escape(status.getTitle())).append("</div>");
                sb.append("</div>");

                sb.append("<div class='customer-detail'>");
                sb.append("Khách: <b>").append(Html.escape(customer.getName())).append("</b> | Vị trí: <span class='table-highlight'>")
                  .append(Html.escape(customer.getTableNumber())).append("</span><br>");
                sb.append("SĐT: ").append(Html.escape(customer.getPhone().isEmpty() ? "Chưa có SĐT" : customer.getPhone()))
                  .append(" | Lúc: ").append(Html.time(o.getCreatedAt()))
                  .append(" | Dự kiến: ~").append(o.estimatePrepMinutes()).append(" phút");
                sb.append("</div>");

                sb.append("<div class='food-items'>");
                for (OrderItem oi : o.getItems()) {
                    sb.append("<div class='food-line'>");
                    sb.append("<span>• ").append(Html.escape(oi.getItem().getName())).append(" x<b class='qty-highlight'>").append(oi.getQuantity()).append("</b></span>");
                    sb.append("<span>").append(Html.money(oi.getSubtotal())).append(" đ</span>");
                    sb.append("</div>");
                    if (!oi.getNote().isBlank()) {
                        sb.append("<div class='item-note'>Ghi chú: ").append(Html.escape(oi.getNote())).append("</div>");
                    }
                }
                sb.append("</div>");
                sb.append("</div>");

                sb.append("<div>");
                sb.append("<div class='ticket-total-row'>");
                sb.append("<span>Tổng cộng:</span>");
                sb.append("<span class='ticket-total-value'>").append(Html.money(o.calculateTotal())).append(" đ</span>");
                sb.append("</div>");

                sb.append("<div class='ticket-actions'>");
                if (status == OrderStatus.PENDING) {
                    sb.append(makeActionButton(o.getOrderId(), OrderStatus.COOKING, "Nhận chế biến", "btn-cooking"));
                } else if (status == OrderStatus.COOKING) {
                    sb.append(makeActionButton(o.getOrderId(), OrderStatus.READY, "Hoàn tất & Trả món", "btn-ready"));
                } else {
                    sb.append("<div class='closed-label'>Đơn đã đóng lưu trữ</div>");
                }
                if (status.canMoveTo(OrderStatus.CANCELLED)) {
                    sb.append(makeActionButton(o.getOrderId(), OrderStatus.CANCELLED, "Hủy đơn", "btn-cancel"));
                }
                sb.append("</div></div>");
                sb.append("</div>");
            }
        }
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String statusClass(OrderStatus status) {
        return switch (status) {
            case PENDING -> "status-pending";
            case COOKING -> "status-cooking";
            case READY -> "status-ready";
            case CANCELLED -> "status-cancelled";
        };
    }

    private static String makeActionButton(int id, OrderStatus targetStatus, String label, String btnClass) {
        return "<form method='POST' action='/status' class='action-form'>"
                + "<input type='hidden' name='orderId' value='" + id + "'>"
                + "<input type='hidden' name='status' value='" + targetStatus.name() + "'>"
                + "<button type='submit' class='btn-cmd " + btnClass + "'>" + Html.escape(label) + "</button>"
                + "</form>";
    }
}
