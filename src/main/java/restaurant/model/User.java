package restaurant.model;

import java.util.Objects;

public abstract class User {
    private final String id;
    private final String name;

    protected User(String id, String name) {
        this.id = Validation.requireNonBlank(id, "Mã định danh người dùng");
        this.name = Validation.requireNonBlank(name, "Tên người dùng");
    }

    public final String getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public boolean canManageKitchen() {
        return false;
    }

    public abstract String getRoleName();

    public abstract String getProfileSummary();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        User that = (User) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public String toString() {
        return getProfileSummary();
    }
}
