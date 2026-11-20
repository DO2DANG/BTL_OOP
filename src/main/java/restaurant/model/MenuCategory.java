package restaurant.model;

import java.util.Optional;

public enum MenuCategory {
    COM_GA("com-ga", "Cơm Gà Đặc Tuyển"),
    MON_GA("mon-ga", "Món Gà Chế Biến"),
    MON_KEM("mon-kem", "Canh & Ăn Kèm"),
    DO_UONG("do-uong", "Trà & Giải Khát");

    private final String code;
    private final String displayName;

    MenuCategory(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<MenuCategory> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.strip();
        for (MenuCategory category : values()) {
            if (category.code.equalsIgnoreCase(normalized)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
