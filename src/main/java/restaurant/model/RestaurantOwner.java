package restaurant.model;

public final class RestaurantOwner extends User {
    public RestaurantOwner(String username, String fullName) {
        super(username, fullName);
    }

    @Override
    public String getRoleName() {
        return "Quản trị / Chủ nhà hàng";
    }
}
