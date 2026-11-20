package restaurant.model;

public final class Beverage extends MenuItem {
    private final boolean servedCold;
    private final int prepMinutes;

    public Beverage(
            int id,
            String name,
            long price,
            String imageUrl,
            String description,
            boolean servedCold,
            int prepMinutes) {
        super(id, name, price, MenuCategory.DO_UONG, imageUrl, description);
        this.servedCold = servedCold;
        this.prepMinutes = Validation.requirePositive(prepMinutes, "Thời gian pha chế");
    }

    public boolean isServedCold() {
        return servedCold;
    }

    @Override
    public int getEstimatedPrepMinutes() {
        return prepMinutes;
    }

    @Override
    public String getServingSpec() {
        return (servedCold ? "Pha lạnh thanh nhiệt" : "Dùng ấm") + " • ~" + prepMinutes + " phút";
    }
}
