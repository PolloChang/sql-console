package work.pollochang.app.sqlconsole.domain;

/**
 * Represents a single SQL statement.
 * Uses SqlContent Value Object for validation.
 */
public record SqlStatement(SqlContent content) {
    
    public SqlStatement(String content) {
        this(new SqlContent(content));
    }

    public boolean isQuery() {
        String sql = content.value().trim().toUpperCase();
        return sql.startsWith("SELECT") || 
               sql.startsWith("WITH") || 
               sql.startsWith("SHOW") || 
               sql.startsWith("DESCRIBE") ||
               sql.startsWith("EXPLAIN");
    }
}
