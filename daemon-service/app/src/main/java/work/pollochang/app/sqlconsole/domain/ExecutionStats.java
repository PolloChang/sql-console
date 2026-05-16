package work.pollochang.app.sqlconsole.domain;

/**
 * Statistics for SQL execution.
 */
public record ExecutionStats(
    long rowCount,
    long updateCount,
    long executionTimeMs,
    String status,
    long totalRows,
    int page,
    int pageSize
) {
    public static ExecutionStats success(long rowCount, long updateCount, long timeMs) {
        return new ExecutionStats(rowCount, updateCount, timeMs, "SUCCESS", rowCount, 1, (int)rowCount);
    }

    public static ExecutionStats success(long rowCount, long updateCount, long timeMs, long totalRows, int page, int pageSize) {
        return new ExecutionStats(rowCount, updateCount, timeMs, "SUCCESS", totalRows, page, pageSize);
    }
}
