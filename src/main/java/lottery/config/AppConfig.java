package lottery.config;

public record AppConfig(
        String databaseUrl,
        String jwtSecret,
        String adminEmail,
        String adminPassword,
        int port
) {
    public static AppConfig load() {
        return new AppConfig(
                env("DATABASE_URL", "jdbc:postgresql://localhost:5432/lottery"),
                env("JWT_SECRET", "super-secret-key"),
                env("ADMIN_EMAIL", "admin@example.com"),
                env("ADMIN_PASSWORD", "admin123"),
                Integer.parseInt(env("PORT", "3000"))
        );
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
