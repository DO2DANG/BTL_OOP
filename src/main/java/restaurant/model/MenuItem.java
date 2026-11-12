package restaurant.model;

public final class MenuItem {
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

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDescription() {
        return description;
    }
}
