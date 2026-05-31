package work.pollochang.app.sqlconsole.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts client-specific SQL commands (like SHOW PARAMETER, DESC)
 * and rewrites them into query-able SQL statements.
 * Implements a pure stateless translator pattern for SOLID compliance.
 */
public class ClientCommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ClientCommandProcessor.class);

    // Loose match patterns to capture any arguments for validation
    private static final Pattern SHOW_PARAMETER_PATTERN = Pattern.compile("(?i)^\\s*show\\s+parameter(?:\\s+(.+))?\\s*$");
    private static final Pattern DESC_PATTERN = Pattern.compile("(?i)^\\s*(?:desc|describe)\\s+(.+)\\s*$");

    public Optional<String> tryRewrite(Connection conn, String sql) {
        if (sql == null || sql.isBlank()) {
            return Optional.empty();
        }

        // Normalize SQL by trimming and removing trailing semicolon if present
        String normalizedSql = sql.trim();
        if (normalizedSql.endsWith(";")) {
            normalizedSql = normalizedSql.substring(0, normalizedSql.length() - 1).trim();
        }

        // 1. Check database dialect
        if (!isOracle(conn)) {
            return Optional.empty();
        }

        // 2. Match SHOW PARAMETER
        Matcher paramMatcher = SHOW_PARAMETER_PATTERN.matcher(normalizedSql);
        if (paramMatcher.matches()) {
            String keyword = paramMatcher.group(1);
            if (keyword != null && !keyword.isBlank()) {
                // Input validation for security
                validateInput(keyword, "^[a-zA-Z0-9_\\-\\.]+$", "Invalid parameter keyword pattern");
                return Optional.of(String.format(
                    "SELECT name, type, value, description FROM v$parameter WHERE name LIKE '%%%s%%' ORDER BY name",
                    keyword.trim().toLowerCase()
                ));
            } else {
                return Optional.of("SELECT name, type, value, description FROM v$parameter ORDER BY name");
            }
        }

        // 3. Match DESC / DESCRIBE
        Matcher descMatcher = DESC_PATTERN.matcher(normalizedSql);
        if (descMatcher.matches()) {
            String tableName = descMatcher.group(1).trim();
            validateInput(tableName, "^[a-zA-Z0-9_\\$#\\.]+$", "Invalid table name pattern");

            String ownerCondition = "";
            String tableOnly = tableName;
            if (tableName.contains(".")) {
                String[] parts = tableName.split("\\.");
                if (parts.length == 2) {
                    ownerCondition = "AND owner = '" + parts[0].trim().toUpperCase() + "' ";
                    tableOnly = parts[1].trim();
                }
            }

            String rewrittenSql = "SELECT column_name AS \"Name\", " +
                    "DECODE(nullable, 'N', 'NOT NULL', '') AS \"Null?\", " +
                    "data_type || " +
                    "CASE WHEN data_type IN ('VARCHAR2', 'CHAR') THEN '(' || data_length || ')' " +
                    "     WHEN data_type = 'NUMBER' AND data_precision IS NOT NULL THEN '(' || data_precision || NVL2(data_scale, ',' || data_scale, '') || ')' " +
                    "     ELSE '' END AS \"Type\" " +
                    "FROM all_tab_columns " +
                    "WHERE table_name = '" + tableOnly.toUpperCase() + "' " +
                    ownerCondition +
                    "ORDER BY column_id";

            return Optional.of(rewrittenSql);
        }

        return Optional.empty();
    }

    private boolean isOracle(Connection conn) {
        try {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            return dbProduct != null && dbProduct.toLowerCase().contains("oracle");
        } catch (SQLException e) {
            logger.warn("Failed to check database product name", e);
            return false;
        }
    }

    private void validateInput(String input, String regex, String errorMsg) {
        if (!input.matches(regex)) {
            throw new IllegalArgumentException(errorMsg + ": " + input);
        }
    }
}
