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
import java.util.Map;

public class TicketCheckHandler implements HttpHandler {
    private final AuthService authService;
    private final TicketService ticketService;

    public TicketCheckHandler(AuthService authService, TicketService ticketService) {
        this.authService = authService;
        this.ticketService = ticketService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            HttpJson.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        UserContext user = AuthSupport.authenticate(exchange, authService);
        if (user == null) return;

        long ticketId = Parsing.queryParamAsLong(exchange.getRequestURI().getQuery(), "ticketId");
        if (ticketId <= 0) {
            HttpJson.sendJson(exchange, 400, Map.of("error", "ticketId is required"));
            return;
        }
        try {
            Map<String, Object> result = ticketService.checkTicket(ticketId, user);
            if (result == null) {
                HttpJson.sendJson(exchange, 404, Map.of("error", "Ticket not found"));
                return;
            }
            HttpJson.sendJson(exchange, 200, result);
        } catch (SecurityException e) {
            HttpJson.sendJson(exchange, 403, Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            HttpJson.sendJson(exchange, 500, Map.of("error", "Database error"));
        }
    }
}
