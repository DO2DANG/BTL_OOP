package restaurant.model;

import java.util.UUID;
import java.util.regex.Pattern;
import restaurant.exception.InvalidPhoneNumberException;

public final class Customer extends User {
    public static final int MAX_NAME_LENGTH = 80;
    public static final int MAX_TABLE_LENGTH = 50;
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:0|\\+84)[1-9][0-9]{8,9}");

    private final String tableNumber;
    private final String phone;

    public Customer(String name, String tableNumber, String phone) throws InvalidPhoneNumberException {
        this(UUID.randomUUID().toString().substring(0, 8), name, tableNumber, phone);
    }

    public Customer(String id, String name, String tableNumber, String phone) throws InvalidPhoneNumberException {
        super(id, Validation.requireNonBlank(name, MAX_NAME_LENGTH, "Tên thực khách"));
        this.tableNumber = Validation.requireNonBlank(tableNumber, MAX_TABLE_LENGTH, "Vị trí bàn hoặc hình thức nhận món");
        this.phone = validatePhone(phone);
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

    @Override
    public String getProfileSummary() {
        String phoneDisplay = phone.isEmpty() ? "Chưa có SĐT" : phone;
        return getName() + " | Vị trí: " + tableNumber + " | SĐT: " + phoneDisplay;
    }
}
