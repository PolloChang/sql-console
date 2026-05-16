package work.pollochang.app.sqlconsole.domain;

/**
 * Value Object for JDBC URL.
 */
public record JdbcUrl(String value) {
    public JdbcUrl {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JDBC URL cannot be empty");
        }
        if (!value.startsWith("jdbc:")) {
            throw new IllegalArgumentException("Invalid JDBC URL format: " + value);
        }
    }
}
