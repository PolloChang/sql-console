package work.pollochang.app.sqlconsole.engine;

import java.util.List;
import java.util.Map;

/**
 * Stores query results for pagination.
 */
public record CachedResult(
    String sqlId,
    List<Map<String, String>> columns,
    List<Map<String, Object>> rows,
    long executionTimeMs
) {}
