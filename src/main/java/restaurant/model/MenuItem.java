package restaurant.model;

import java.util.Objects;

public abstract class MenuItem {
    private final int id;
    private final String name;
    private final long price;
    private final MenuCategory category;
    private final String imageUrl;
    private final String description;

    protected MenuItem(
            int id,
            String name,
            long price,
            MenuCategory category,
            String imageUrl,
            String description) {
        this.id = Validation.requirePositive(id, "Mã món ăn");
        this.name = Validation.requireNonBlank(name, "Tên món ăn");
        this.price = Validation.requirePositive(price, "Đơn giá món ăn");
        this.category = Objects.requireNonNull(category, "Danh mục món ăn không được để trống.");
        this.imageUrl = Validation.requireNonBlank(imageUrl, "Đường dẫn hình ảnh");
        this.description = Validation.requireNonBlank(description, "Mô tả món ăn");
    }

    public final int getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public final long getPrice() {
        return price;
    }

    public final MenuCategory getCategory() {
        return category;
    }

    public final String getImageUrl() {
        return imageUrl;
    }

    public final String getDescription() {
        return description;
    }

    public abstract int getEstimatedPrepMinutes();

    public abstract String getServingSpec();

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MenuItem that)) {
            return false;
        }
        return id == that.id;
    }

    @Override
    public final int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "#" + id + " " + name + " (" + price + " đ)";
    }
}
