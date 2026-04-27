package lottery.db;

import lottery.config.AppConfig;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class MigrationService {
    private final AppConfig config;

    public MigrationService(AppConfig config) {
        this.config = config;
    }

    public void migrate() throws SQLException {
        try (Connection c = Db.conn(config); Statement st = c.createStatement()) {
            st.execute("""
                    create table if not exists users(
                      id bigserial primary key,
                      email text unique not null,
                      password_hash text not null,
                      role text not null check (role in ('ADMIN','USER')),
                      created_at timestamp default now()
                    );
                    """);
            st.execute("""
                    create table if not exists draws(
                      id bigserial primary key,
                      title text not null,
                      status text not null check (status in ('ACTIVE','COMPLETED')),
                      created_by bigint not null references users(id) on delete restrict,
                      created_at timestamp default now()
                    );
                    """);
            st.execute("""
                    create table if not exists draw_results(
                      id bigserial primary key,
                      draw_id bigint unique not null references draws(id) on delete cascade,
                      winning_numbers text not null,
                      created_at timestamp default now()
                    );
                    """);
            st.execute("""
                    create table if not exists tickets(
                      id bigserial primary key,
                      user_id bigint not null references users(id) on delete cascade,
                      draw_id bigint not null references draws(id) on delete cascade,
                      numbers text not null,
                      status text not null check (status in ('PENDING','WIN','LOSE')),
                      created_at timestamp default now()
                    );
                    """);
            st.execute("""
                    create table if not exists payments(
                      id bigserial primary key,
                      ticket_id bigint unique references tickets(id) on delete cascade,
                      amount numeric(10,2) not null,
                      status text not null check (status in ('PAID','FAILED')),
                      created_at timestamp default now()
                    );
                    """);
        }
    }

    public void seedAdmin() throws SQLException {
        try (Connection c = Db.conn(config);
             PreparedStatement check = c.prepareStatement("select id from users where email=?")) {
            check.setString(1, config.adminEmail().toLowerCase(Locale.ROOT));
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                return;
            }
        }
        try (Connection c = Db.conn(config);
             PreparedStatement insert = c.prepareStatement("insert into users(email, password_hash, role) values(?, ?, 'ADMIN')")) {
            insert.setString(1, config.adminEmail().toLowerCase(Locale.ROOT));
            insert.setString(2, BCrypt.hashpw(config.adminPassword(), BCrypt.gensalt()));
            insert.executeUpdate();
        }
    }
}
