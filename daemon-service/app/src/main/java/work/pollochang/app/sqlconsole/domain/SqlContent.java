package work.pollochang.app.sqlconsole.domain;

/**
 * Value Object for SQL Statement content.
 */
public record SqlContent(String value) {
    public SqlContent {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL content cannot be empty");
        }
    }
}
