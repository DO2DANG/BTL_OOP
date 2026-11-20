package restaurant.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import restaurant.exception.InvalidPhoneNumberException;
import restaurant.model.Customer;
import restaurant.model.MenuItem;
import restaurant.model.Order;
import restaurant.model.OrderItem;
import restaurant.model.OrderStatus;

public final class FileOrderRepository implements OrderRepository {
    private static final int INITIAL_ORDER_ID = 501;

    private final Path storageFile;
    private final MenuRepository menuRepository;
    private final List<Order> orders = new ArrayList<>();
    private int sequence = INITIAL_ORDER_ID;

    public FileOrderRepository(Path storageFile, MenuRepository menuRepository) {
        this.storageFile = Objects.requireNonNull(storageFile, "Đường dẫn tệp lưu trữ không được để trống.");
        this.menuRepository = Objects.requireNonNull(menuRepository, "Kho thực đơn không được để trống.");
        loadFromDisk();
    }

    @Override
    public synchronized int nextOrderId() {
        return sequence++;
    }

    @Override
    public synchronized void save(Order order) {
        Objects.requireNonNull(order, "Đơn hàng lưu trữ không được để trống.");
        boolean updated = false;
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).getOrderId() == order.getOrderId()) {
                orders.set(i, order);
                updated = true;
                break;
            }
        }
        if (!updated) {
            orders.add(0, order);
        }
        if (order.getOrderId() >= sequence) {
            sequence = order.getOrderId() + 1;
        }
        flushToDisk();
    }

    @Override
    public synchronized Optional<Order> findById(int orderId) {
        return orders.stream().filter(order -> order.getOrderId() == orderId).findFirst();
    }

    @Override
    public synchronized List<Order> findAll() {
        return List.copyOf(orders);
    }

    private void loadFromDisk() {
        if (!Files.exists(storageFile)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(storageFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                Order parsed = deserializeOrder(line);
                if (parsed != null) {
                    orders.add(parsed);
                    if (parsed.getOrderId() >= sequence) {
                        sequence = parsed.getOrderId() + 1;
                    }
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Không thể đọc tệp đơn hàng: " + storageFile, exception);
        }
    }

    private void flushToDisk() {
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            List<String> lines = orders.stream().map(this::serializeOrder).toList();
            Path tempFile = storageFile.resolveSibling(storageFile.getFileName() + ".tmp");
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            Files.move(tempFile, storageFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException("Không thể ghi tệp đơn hàng: " + storageFile, exception);
        }
    }

    private String serializeOrder(Order order) {
        Customer customer = order.getCustomer();
        StringBuilder itemBuilder = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            if (!itemBuilder.isEmpty()) {
                itemBuilder.append(',');
            }
            itemBuilder.append(item.getItem().getId())
                    .append(':')
                    .append(item.getQuantity())
                    .append(':')
                    .append(encode(item.getNote()));
        }
        return order.getOrderId()
                + "|" + order.getStatus().name()
                + "|" + order.getCreatedAt()
                + "|" + encode(customer.getId())
                + "|" + encode(customer.getName())
                + "|" + encode(customer.getTableNumber())
                + "|" + encode(customer.getPhone())
                + "|" + itemBuilder;
    }

    private Order deserializeOrder(String line) {
        String[] parts = line.split("\\|", 8);
        if (parts.length < 8) {
            return null;
        }
        try {
            int orderId = Integer.parseInt(parts[0]);
            OrderStatus status = OrderStatus.valueOf(parts[1]);
            LocalDateTime createdAt = LocalDateTime.parse(parts[2]);
            Customer customer = new Customer(
                    decode(parts[3]),
                    decode(parts[4]),
                    decode(parts[5]),
                    decode(parts[6]));

            List<OrderItem> items = new ArrayList<>();
            for (String token : parts[7].split(",")) {
                if (token.isBlank()) {
                    continue;
                }
                String[] itemParts = token.split(":", 3);
                int productId = Integer.parseInt(itemParts[0]);
                int quantity = Integer.parseInt(itemParts[1]);
                String note = itemParts.length == 3 ? decode(itemParts[2]) : "";
                MenuItem menuItem = menuRepository.findById(productId).orElse(null);
                if (menuItem != null) {
                    items.add(new OrderItem(menuItem, quantity, note));
                }
            }
            if (items.isEmpty()) {
                return null;
            }
            return new Order(orderId, customer, items, createdAt, status);
        } catch (RuntimeException | InvalidPhoneNumberException ignored) {
            return null;
        }
    }

    private static String encode(String text) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
