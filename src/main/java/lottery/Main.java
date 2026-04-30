package lottery;

import lottery.auth.AuthService;
import lottery.config.AppConfig;
import lottery.db.MigrationService;
import lottery.handler.AuthHandler;
import lottery.handler.DrawHandler;
import lottery.handler.HealthHandler;
import lottery.handler.TicketCheckHandler;
import lottery.handler.TicketHandler;
import lottery.service.DrawService;
import lottery.service.TicketService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        MigrationService migrationService = new MigrationService(config);
        migrationService.migrate();
        migrationService.seedAdmin();

        AuthService authService = new AuthService(config);
        DrawService drawService = new DrawService(config);
        TicketService ticketService = new TicketService(config);

        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/health", new HealthHandler());
        server.createContext("/auth/register", new AuthHandler.RegisterHandler(config));
        server.createContext("/auth/login", new AuthHandler.LoginHandler(config, authService));
        server.createContext("/draws", new DrawHandler.DrawsCrudHandler(authService, drawService));
        server.createContext("/draws/generate-result", new DrawHandler.DrawResultHandler(authService, drawService));
        server.createContext("/tickets", new TicketHandler(authService, ticketService));
        server.createContext("/tickets/check", new TicketCheckHandler(authService, ticketService));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Lottery API started at :" + config.port());
    }
}
