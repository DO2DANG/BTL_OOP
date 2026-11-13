package restaurant.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class HttpSupport {
    private static final int MAX_FORM_BYTES = 8192;

    private HttpSupport() {
    }

    static void register(HttpServer server, String path, String method, HttpHandler handler) {
        server.createContext(path, exchange -> {
            try {
                if (!path.equals(exchange.getRequestURI().getPath())) {
                    text(exchange, 404, "Không tìm thấy trang.");
                    return;
                }
                if (!method.equals(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Allow", method);
                    text(exchange, 405, "Phương thức HTTP không được hỗ trợ.");
                    return;
                }
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
    }

    static void registerAsset(HttpServer server, String path, String contentType) throws IOException {
        try (InputStream input = HttpSupport.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Thiếu tệp giao diện " + path);
            }
            byte[] content = input.readAllBytes();
            register(server, path, "GET", exchange -> send(exchange, 200, contentType, content));
        }
    }

    static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        byte[] bytes;
        try (InputStream input = exchange.getRequestBody()) {
            bytes = input.readNBytes(MAX_FORM_BYTES + 1);
        }
        if (bytes.length > MAX_FORM_BYTES) {
            throw new IllegalArgumentException("Dữ liệu gửi lên quá dài.");
        }

        Map<String, String> fields = new HashMap<>();
        String body = new String(bytes, StandardCharsets.UTF_8);
        try {
            for (String pair : body.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                String[] parts = pair.split("=", 2);
                String name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                fields.put(name, value);
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Dữ liệu biểu mẫu không hợp lệ.", exception);
        }
        return fields;
    }

    static int readNumber(Map<String, String> fields, String field, String errorMessage) {
        try {
            return Integer.parseInt(fields.get(field));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage, exception);
        }
    }

    static void html(HttpExchange exchange, int status, String content) throws IOException {
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        send(exchange, status, "text/html; charset=UTF-8", content.getBytes(StandardCharsets.UTF_8));
    }

    static void text(HttpExchange exchange, int status, String content) throws IOException {
        send(exchange, status, "text/plain; charset=UTF-8", content.getBytes(StandardCharsets.UTF_8));
    }

    static void redirect(HttpExchange exchange, String destination) throws IOException {
        exchange.getResponseHeaders().set("Location", destination);
        exchange.sendResponseHeaders(303, -1);
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] content) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }
}
