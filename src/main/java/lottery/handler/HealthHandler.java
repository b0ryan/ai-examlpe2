package lottery.handler;

import lottery.util.HttpJson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpJson.sendJson(exchange, 200, Map.of("status", "ok"));
    }
}
