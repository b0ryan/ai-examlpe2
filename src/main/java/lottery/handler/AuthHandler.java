package lottery.handler;

import lottery.auth.AuthService;
import lottery.config.AppConfig;
import lottery.service.AuthUserService;
import lottery.util.HttpJson;
import lottery.util.Parsing;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public final class AuthHandler {
    private AuthHandler() {}

    public static class RegisterHandler implements HttpHandler {
        private final AuthUserService authUserService;

        public RegisterHandler(AppConfig config) {
            this.authUserService = new AuthUserService(config);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                HttpJson.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }
            Map<String, Object> body = HttpJson.readJson(exchange);
            String email = Parsing.str(body.get("email"));
            String password = Parsing.str(body.get("password"));
            if (email.isBlank() || password.isBlank()) {
                HttpJson.sendJson(exchange, 400, Map.of("error", "email and password are required"));
                return;
            }
            try {
                long id = authUserService.register(email, password);
                HttpJson.sendJson(exchange, 201, Map.of("id", id, "email", email, "role", "USER"));
            } catch (SQLException e) {
                HttpJson.sendJson(exchange, 409, Map.of("error", "User already exists"));
            }
        }
    }

    public static class LoginHandler implements HttpHandler {
        private final AuthUserService authUserService;
        private final AuthService authService;

        public LoginHandler(AppConfig config, AuthService authService) {
            this.authUserService = new AuthUserService(config);
            this.authService = authService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                HttpJson.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }
            Map<String, Object> body = HttpJson.readJson(exchange);
            String email = Parsing.str(body.get("email"));
            String password = Parsing.str(body.get("password"));

            try {
                AuthUserService.LoginResult loginResult = authUserService.login(email, password);
                if (loginResult == null) {
                    HttpJson.sendJson(exchange, 401, Map.of("error", "Invalid credentials"));
                    return;
                }
                String token = authService.issueToken(loginResult.userId(), loginResult.role());
                HttpJson.sendJson(exchange, 200, Map.of("token", token, "role", loginResult.role()));
            } catch (SQLException e) {
                HttpJson.sendJson(exchange, 500, Map.of("error", "Database error"));
            }
        }
    }
}
