package lottery.handler;

import lottery.auth.AuthService;
import lottery.auth.UserContext;
import lottery.util.HttpJson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public final class AuthSupport {
    private AuthSupport() {}

    public static UserContext authenticate(HttpExchange exchange, AuthService authService) throws IOException {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            HttpJson.sendJson(exchange, 401, Map.of("error", "Missing bearer token"));
            return null;
        }
        try {
            return authService.verify(header.substring("Bearer ".length()));
        } catch (Exception e) {
            HttpJson.sendJson(exchange, 401, Map.of("error", "Invalid token"));
            return null;
        }
    }
}
