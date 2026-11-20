package restaurant.model;

public final class ChickenDish extends MenuItem {
    private final String cookingStyle;
    private final int prepMinutes;

    public ChickenDish(
            int id,
            String name,
            long price,
            MenuCategory category,
            String imageUrl,
            String description,
            String cookingStyle,
            int prepMinutes) {
        super(id, name, price, category, imageUrl, description);
        this.cookingStyle = Validation.requireNonBlank(cookingStyle, "Kiểu chế biến món gà");
        this.prepMinutes = Validation.requirePositive(prepMinutes, "Thời gian chế biến");
    }

    public String getCookingStyle() {
        return cookingStyle;
    }

    @Override
    public int getEstimatedPrepMinutes() {
        return prepMinutes;
    }

    @Override
    public String getServingSpec() {
        return cookingStyle + " • ~" + prepMinutes + " phút";
    }
}
