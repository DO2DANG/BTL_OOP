package restaurant.model;

public final class SideDish extends MenuItem {
    private final String servingPortion;
    private final int prepMinutes;

    public SideDish(
            int id,
            String name,
            long price,
            String imageUrl,
            String description,
            String servingPortion,
            int prepMinutes) {
        super(id, name, price, MenuCategory.MON_KEM, imageUrl, description);
        this.servingPortion = Validation.requireNonBlank(servingPortion, "Khẩu phần món kèm");
        this.prepMinutes = Validation.requirePositive(prepMinutes, "Thời gian chuẩn bị món kèm");
    }

    public String getServingPortion() {
        return servingPortion;
    }

    @Override
    public int getEstimatedPrepMinutes() {
        return prepMinutes;
    }

    @Override
    public String getServingSpec() {
        return servingPortion + " • ~" + prepMinutes + " phút";
    }
}
