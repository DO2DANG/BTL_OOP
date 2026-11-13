package restaurant.model;

public abstract class User {
    private final String id;
    private final String name;

    protected User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public abstract String getRoleName();
}
