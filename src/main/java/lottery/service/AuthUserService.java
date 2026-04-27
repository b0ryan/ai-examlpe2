package lottery.service;

import lottery.config.AppConfig;
import lottery.db.Db;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class AuthUserService {
    private final AppConfig config;

    public AuthUserService(AppConfig config) {
        this.config = config;
    }

    public long register(String email, String password) throws SQLException {
        try (Connection c = Db.conn(config);
             PreparedStatement ps = c.prepareStatement(
                     "insert into users(email, password_hash, role) values (?, ?, 'USER') returning id")) {
            ps.setString(1, email.toLowerCase(Locale.ROOT));
            ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getLong("id");
        }
    }

    public LoginResult login(String email, String password) throws SQLException {
        try (Connection c = Db.conn(config);
             PreparedStatement ps = c.prepareStatement("select id, password_hash, role from users where email=?")) {
            ps.setString(1, email.toLowerCase(Locale.ROOT));
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            if (!BCrypt.checkpw(password, rs.getString("password_hash"))) {
                return null;
            }
            return new LoginResult(rs.getLong("id"), rs.getString("role"));
        }
    }

    public record LoginResult(long userId, String role) {}
}
