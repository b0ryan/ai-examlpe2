package lottery.service;

import lottery.config.AppConfig;
import lottery.db.Db;
import lottery.util.LotteryNumbers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DrawService {
    private final AppConfig config;

    public DrawService(AppConfig config) {
        this.config = config;
    }

    public Map<String, Object> createDraw(String title, long createdBy) throws SQLException {
        try (Connection c = Db.conn(config);
             PreparedStatement ps = c.prepareStatement(
                     "insert into draws(title, status, created_by) values(?, 'ACTIVE', ?) returning id, status")) {
            ps.setString(1, title);
            ps.setLong(2, createdBy);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return Map.of("id", rs.getLong("id"), "title", title, "status", rs.getString("status"));
        }
    }

    public List<Map<String, Object>> listActiveDraws() throws SQLException {
        List<Map<String, Object>> draws = new ArrayList<>();
        try (Connection c = Db.conn(config);
             PreparedStatement ps = c.prepareStatement("select id, title, status, created_at from draws where status='ACTIVE' order by id desc")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                draws.add(Map.of(
                        "id", rs.getLong("id"),
                        "title", rs.getString("title"),
                        "status", rs.getString("status"),
                        "createdAt", rs.getTimestamp("created_at").toInstant().toString()
                ));
            }
        }
        return draws;
    }

    public boolean deleteDraw(long drawId) throws SQLException {
        try (Connection c = Db.conn(config);
             PreparedStatement ps = c.prepareStatement("delete from draws where id=?")) {
            ps.setLong(1, drawId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Integer> generateResultForDraw(long drawId) throws SQLException {
        try (Connection c = Db.conn(config)) {
            c.setAutoCommit(false);
            try {
                String status;
                try (PreparedStatement drawPs = c.prepareStatement("select status from draws where id=? for update")) {
                    drawPs.setLong(1, drawId);
                    ResultSet rs = drawPs.executeQuery();
                    if (!rs.next()) {
                        c.rollback();
                        return null;
                    }
                    status = rs.getString("status");
                }

                // Idempotency: if result already exists, return it without reprocessing tickets.
                try (PreparedStatement existingResultPs = c.prepareStatement(
                        "select winning_numbers from draw_results where draw_id=?")) {
                    existingResultPs.setLong(1, drawId);
                    ResultSet existingRs = existingResultPs.executeQuery();
                    if (existingRs.next()) {
                        List<Integer> existing = LotteryNumbers.parseNumbers(existingRs.getString("winning_numbers"));
                        c.commit();
                        return existing;
                    }
                }

                if (!"ACTIVE".equals(status)) {
                    c.rollback();
                    throw new IllegalStateException("Draw is not active and has no result");
                }

                List<Integer> combo = LotteryNumbers.generateWinningCombo();
                String comboText = LotteryNumbers.joinNumbers(combo);

                try (PreparedStatement insertResult = c.prepareStatement(
                        "insert into draw_results(draw_id, winning_numbers) values(?, ?)")) {
                    insertResult.setLong(1, drawId);
                    insertResult.setString(2, comboText);
                    insertResult.executeUpdate();
                }

                try (PreparedStatement ticketSelect = c.prepareStatement("select id, numbers from tickets where draw_id=?")) {
                    ticketSelect.setLong(1, drawId);
                    ResultSet tickets = ticketSelect.executeQuery();
                    while (tickets.next()) {
                        String result = LotteryNumbers.isMatch(tickets.getString("numbers"), comboText) ? "WIN" : "LOSE";
                        try (PreparedStatement ticketUpdate = c.prepareStatement("update tickets set status=? where id=?")) {
                            ticketUpdate.setString(1, result);
                            ticketUpdate.setLong(2, tickets.getLong("id"));
                            ticketUpdate.executeUpdate();
                        }
                    }
                }

                try (PreparedStatement closeDraw = c.prepareStatement("update draws set status='COMPLETED' where id=?")) {
                    closeDraw.setLong(1, drawId);
                    closeDraw.executeUpdate();
                }

                c.commit();
                return combo;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
