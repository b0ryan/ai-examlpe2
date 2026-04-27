package lottery.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class HttpJson {
    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpJson() {}

    public static Map<String, Object> readJson(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            byte[] bytes = input.readAllBytes();
            if (bytes.length == 0) {
                return new HashMap<>();
            }
            return JSON.readValue(bytes, new TypeReference<>() {});
        }
    }

    public static void sendJson(HttpExchange exchange, int code, Map<String, ?> body) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
