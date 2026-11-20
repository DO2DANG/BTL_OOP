package restaurant.model;

final class Validation {
    private Validation() {
    }

    static String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " là thông tin bắt buộc.");
        }
        return value.strip();
    }

    static int requirePositive(int value, String label) {
        if (value <= 0) {
            throw new IllegalArgumentException(label + " phải lớn hơn 0.");
        }
        return value;
    }

    static long requirePositive(long value, String label) {
        if (value <= 0L) {
            throw new IllegalArgumentException(label + " phải lớn hơn 0.");
        }
        return value;
    }
}
