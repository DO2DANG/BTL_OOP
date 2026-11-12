package vn.edu.ptit.restaurant.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CartSession {
    private final Map<Integer, OrderItem> items = new LinkedHashMap<>();

    public synchronized void addProduct(MenuItem product, String note) {
        int productId = product.getId();
        OrderItem current = items.get(productId);
        if (current == null) {
            items.put(productId, new OrderItem(product, 1, note));
            return;
        }
        String updatedNote = note == null || note.isBlank() ? current.getNote() : note;
        items.put(productId, new OrderItem(product, current.getQuantity() + 1, updatedNote));
    }

    public synchronized boolean changeQty(int productId, int delta) {
        if (delta != 1 && delta != -1) {
            throw new IllegalArgumentException("Số lượng mỗi lần chỉ được tăng hoặc giảm 1 phần.");
        }
        OrderItem current = items.get(productId);
        if (current == null) {
            return false;
        }
        int updatedQuantity = current.getQuantity() + delta;
        if (updatedQuantity == 0) {
            items.remove(productId);
        } else {
            items.put(productId, new OrderItem(current.getItem(), updatedQuantity, current.getNote()));
        }
        return true;
    }

    public synchronized List<OrderItem> getItems() {
        return List.copyOf(items.values());
    }

    public synchronized int getTotalQuantity() {
        return items.values().stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public synchronized long getTotalPrice() {
        return items.values().stream().mapToLong(OrderItem::getSubtotal).sum();
    }

    public synchronized void clear() {
        items.clear();
    }
}
