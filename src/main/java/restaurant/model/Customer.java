package restaurant.model;

import java.util.UUID;
import java.util.regex.Pattern;
import restaurant.exception.InvalidPhoneNumberException;

public final class Customer extends User {
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:0|\\+84)[1-9][0-9]{8,9}");

    private final String tableNumber;
    private final String phone;

    public Customer(String name, String tableNumber, String phone) throws InvalidPhoneNumberException {
        super(UUID.randomUUID().toString().substring(0, 8), requireField(name, "Tên thực khách"));
        this.tableNumber = requireField(tableNumber, "Vị trí bàn hoặc hình thức nhận món");
        this.phone = validatePhone(phone);
    }

    private static String requireField(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " là thông tin bắt buộc.");
        }
        return value.strip();
    }

    private static String validatePhone(String phone) throws InvalidPhoneNumberException {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String normalized = phone.strip().replace(" ", "").replace(".", "").replace("-", "");
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new InvalidPhoneNumberException();
        }
        return normalized;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String getRoleName() {
        return "Khách hàng";
    }
}
