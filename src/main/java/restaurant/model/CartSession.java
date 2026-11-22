package restaurant.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import restaurant.exception.EmptyCartException;

public final class CartSession {
    public record Snapshot(List<OrderItem> items, int totalQuantity, long totalPrice) {
        public Snapshot {
            items = List.copyOf(items);
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }
    }

    private final Map<Integer, OrderItem> items = new LinkedHashMap<>();

    public synchronized void addProduct(MenuItem product, String note) {
        Objects.requireNonNull(product, "Món thêm vào giỏ không được để trống.");
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
        return items.values().stream().mapToLong(OrderItem::getSubtotal).max().isEmpty()
                ? 0L
                : items.values().stream().mapToLong(OrderItem::getSubtotal).reduce(0L, Math::addExact);
    }

    public synchronized Snapshot snapshot() {
        List<OrderItem> currentItems = List.copyOf(items.values());
        int totalQty = currentItems.stream().mapToInt(OrderItem::getQuantity).sum();
        long totalPrice = currentItems.stream().mapToLong(OrderItem::getSubtotal).reduce(0L, Math::addExact);
        return new Snapshot(currentItems, totalQty, totalPrice);
    }

    public synchronized List<OrderItem> drainItems() throws EmptyCartException {
        if (items.isEmpty()) {
            throw new EmptyCartException();
        }
        List<OrderItem> drained = List.copyOf(items.values());
        items.clear();
        return drained;
    }

    public synchronized void clear() {
        items.clear();
    }
}
