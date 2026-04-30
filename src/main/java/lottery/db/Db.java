package lottery.db;

import lottery.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {}

    public static Connection conn(AppConfig config) throws SQLException {
        return DriverManager.getConnection(config.databaseUrl());
    }
}
