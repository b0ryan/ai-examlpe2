package lottery.service;

import lottery.auth.UserContext;
import lottery.config.AppConfig;
import lottery.db.Db;
import lottery.util.LotteryNumbers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TicketService {
    private final AppConfig config;

    public TicketService(AppConfig config) {
        this.config = config;
    }

    public long createTicket(long drawId, List<Integer> numbers, long userId) throws SQLException {
        try (Connection c = Db.conn(config)) {
            try (PreparedStatement drawPs = c.prepareStatement("select status from draws where id=?")) {
                drawPs.setLong(1, drawId);
                ResultSet drawRs = drawPs.executeQuery();
                if (!drawRs.next()) {
                    throw new IllegalArgumentException("Draw not found");
                }
                if (!"ACTIVE".equals(drawRs.getString("status"))) {
                    throw new IllegalStateException("Draw is not active");
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "insert into tickets(user_id, draw_id, numbers, status) values(?, ?, ?, 'PENDING') returning id")) {
                ps.setLong(1, userId);
                ps.setLong(2, drawId);
                ps.setString(3, LotteryNumbers.joinNumbers(numbers));
                ResultSet rs = ps.executeQuery();
                rs.next();
                return rs.getLong("id");
            }
        }
    }

    public Map<String, Object> checkTicket(long ticketId, UserContext user) throws SQLException {
        try (Connection c = Db.conn(config);
             PreparedStatement ps = c.prepareStatement(
                     "select t.id, t.user_id, t.draw_id, t.numbers, t.status, d.status as draw_status " +
                             "from tickets t join draws d on d.id=t.draw_id where t.id=?")) {
            ps.setLong(1, ticketId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            if (!"ADMIN".equals(user.role()) && rs.getLong("user_id") != user.userId()) {
                throw new SecurityException("Forbidden");
            }
            return Map.of(
                    "ticketId", rs.getLong("id"),
                    "drawId", rs.getLong("draw_id"),
                    "numbers", LotteryNumbers.parseNumbers(rs.getString("numbers")),
                    "ticketStatus", rs.getString("status"),
                    "drawStatus", rs.getString("draw_status")
            );
        }
    }
}
