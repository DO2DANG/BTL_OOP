package vn.edu.ptit.restaurant.model;

import java.util.Objects;

public final class OrderItem {
    private final MenuItem item;
    private final int quantity;
    private final String note;

    public OrderItem(MenuItem item, int quantity, String note) {
        this.item = Objects.requireNonNull(item);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng món phải lớn hơn 0.");
        }
        this.quantity = quantity;
        this.note = note == null ? "" : note.strip();
    }

    public MenuItem getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getNote() {
        return note;
    }

    public long getSubtotal() {
        return item.getPrice() * quantity;
    }
}
