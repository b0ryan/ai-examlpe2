package lottery.handler;

import lottery.auth.AuthService;
import lottery.auth.UserContext;
import lottery.service.TicketService;
import lottery.util.HttpJson;
import lottery.util.Parsing;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TicketHandler implements HttpHandler {
    private final AuthService authService;
    private final TicketService ticketService;

    public TicketHandler(AuthService authService, TicketService ticketService) {
        this.authService = authService;
        this.ticketService = ticketService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            HttpJson.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        UserContext user = AuthSupport.authenticate(exchange, authService);
        if (user == null) return;

        Map<String, Object> body = HttpJson.readJson(exchange);
        long drawId = Parsing.asLong(body.get("drawId"));
        List<Integer> numbers = Parsing.toIntList(body.get("numbers"));
        if (drawId <= 0 || numbers.size() != 5) {
            HttpJson.sendJson(exchange, 400, Map.of("error", "drawId and exactly 5 numbers are required"));
            return;
        }
        for (int number : numbers) {
            if (number < 1 || number > 50) {
                HttpJson.sendJson(exchange, 400, Map.of("error", "numbers must be in range 1..50"));
                return;
            }
        }
        if (numbers.stream().distinct().count() != 5) {
            HttpJson.sendJson(exchange, 400, Map.of("error", "numbers must be unique"));
            return;
        }
        try {
            long ticketId = ticketService.createTicket(drawId, numbers, user.userId());
            HttpJson.sendJson(exchange, 201, Map.of("id", ticketId, "status", "PENDING"));
        } catch (IllegalArgumentException e) {
            HttpJson.sendJson(exchange, 404, Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            HttpJson.sendJson(exchange, 409, Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            HttpJson.sendJson(exchange, 500, Map.of("error", "Database error"));
        }
    }
}
