package lottery.handler;

import lottery.auth.AuthService;
import lottery.auth.UserContext;
import lottery.service.DrawService;
import lottery.util.HttpJson;
import lottery.util.Parsing;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class DrawHandler {
    private DrawHandler() {}

    public static class DrawsCrudHandler implements HttpHandler {
        private final AuthService authService;
        private final DrawService drawService;

        public DrawsCrudHandler(AuthService authService, DrawService drawService) {
            this.authService = authService;
            this.drawService = drawService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                createDraw(exchange);
                return;
            }
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                listDraws(exchange);
                return;
            }
            if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                deleteDraw(exchange);
                return;
            }
            HttpJson.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
        }

        private void createDraw(HttpExchange exchange) throws IOException {
            UserContext user = AuthSupport.authenticate(exchange, authService);
            if (user == null) return;
            if (!"ADMIN".equals(user.role())) {
                HttpJson.sendJson(exchange, 403, Map.of("error", "Admin only"));
                return;
            }
            Map<String, Object> body = HttpJson.readJson(exchange);
            String title = Parsing.str(body.get("title"));
            if (title.isBlank()) title = "Draw";
            try {
                HttpJson.sendJson(exchange, 201, drawService.createDraw(title, user.userId()));
            } catch (SQLException e) {
                HttpJson.sendJson(exchange, 500, Map.of("error", "Database error"));
            }
        }

        private void listDraws(HttpExchange exchange) throws IOException {
            UserContext user = AuthSupport.authenticate(exchange, authService);
            if (user == null) return;
            try {
                List<Map<String, Object>> draws = drawService.listActiveDraws();
                HttpJson.sendJson(exchange, 200, Map.of("items", draws));
            } catch (SQLException e) {
                HttpJson.sendJson(exchange, 500, Map.of("error", "Database error"));
            }
        }

        private void deleteDraw(HttpExchange exchange) throws IOException {
            UserContext user = AuthSupport.authenticate(exchange, authService);
            if (user == null) return;
            if (!"ADMIN".equals(user.role())) {
                HttpJson.sendJson(exchange, 403, Map.of("error", "Admin only"));
                return;
            }
            long drawId = Parsing.queryParamAsLong(exchange.getRequestURI().getQuery(), "id");
            if (drawId <= 0) {
                HttpJson.sendJson(exchange, 400, Map.of("error", "id is required"));
                return;
            }
            try {
                boolean deleted = drawService.deleteDraw(drawId);
                if (!deleted) {
                    HttpJson.sendJson(exchange, 404, Map.of("error", "Draw not found"));
                    return;
                }
                HttpJson.sendJson(exchange, 200, Map.of("status", "deleted"));
            } catch (SQLException e) {
                HttpJson.sendJson(exchange, 500, Map.of("error", "Database error"));
            }
        }
    }

    public static class DrawResultHandler implements HttpHandler {
        private final AuthService authService;
        private final DrawService drawService;

        public DrawResultHandler(AuthService authService, DrawService drawService) {
            this.authService = authService;
            this.drawService = drawService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                HttpJson.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }
            UserContext user = AuthSupport.authenticate(exchange, authService);
            if (user == null) return;
            if (!"ADMIN".equals(user.role())) {
                HttpJson.sendJson(exchange, 403, Map.of("error", "Admin only"));
                return;
            }
            long drawId = Parsing.asLong(HttpJson.readJson(exchange).get("drawId"));
            if (drawId <= 0) {
                HttpJson.sendJson(exchange, 400, Map.of("error", "drawId is required"));
                return;
            }
            try {
                List<Integer> combo = drawService.generateResultForDraw(drawId);
                if (combo == null) {
                    HttpJson.sendJson(exchange, 404, Map.of("error", "Draw not found"));
                    return;
                }
                HttpJson.sendJson(exchange, 200, Map.of("drawId", drawId, "winningNumbers", combo, "drawStatus", "COMPLETED"));
            } catch (IllegalStateException e) {
                HttpJson.sendJson(exchange, 409, Map.of("error", e.getMessage()));
            } catch (SQLException e) {
                HttpJson.sendJson(exchange, 500, Map.of("error", "Database error"));
            }
        }
    }
}
