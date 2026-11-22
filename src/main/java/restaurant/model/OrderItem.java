package restaurant.model;

import java.util.Objects;

public final class OrderItem {
    public static final int MAX_QUANTITY = 99;
    public static final int MAX_NOTE_LENGTH = 200;

    private final MenuItem item;
    private final int quantity;
    private final String note;

    public OrderItem(MenuItem item, int quantity, String note) {
        this.item = Objects.requireNonNull(item, "Món ăn trong dòng đơn không được để trống.");
        this.quantity = Validation.requirePositive(quantity, "Số lượng món");
        if (this.quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Mỗi món chỉ đặt tối đa " + MAX_QUANTITY + " phần trong một đơn.");
        }
        this.note = Validation.requireMaxLength(note, MAX_NOTE_LENGTH, "Ghi chú món ăn");
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
        return Math.multiplyExact(item.getPrice(), (long) quantity);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OrderItem that)) {
            return false;
        }
        return quantity == that.quantity
                && item.equals(that.item)
                && note.equals(that.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, quantity, note);
    }

    @Override
    public String toString() {
        return item.getName() + " x" + quantity + (note.isEmpty() ? "" : " (" + note + ")");
    }
}
