package work.pollochang.app.sqlconsole.engine;

import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.RequestId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Decorator that collects rows to create a CachedResult upon completion.
 * Can also limit the number of rows delegated to the underlying handler for pagination.
 */
public class CachingResultHandler implements ResultHandler {
    private final ResultHandler delegate;
    private final Consumer<CachedResult> onComplete;
    private final int limit;
    private String sqlId;
    private List<Map<String, String>> columns;
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private int delegatedCount = 0;

    public CachingResultHandler(ResultHandler delegate, Consumer<CachedResult> onComplete) {
        this(delegate, onComplete, -1);
    }

    public CachingResultHandler(ResultHandler delegate, Consumer<CachedResult> onComplete, int limit) {
        this.delegate = delegate;
        this.onComplete = onComplete;
        this.limit = limit;
    }

    @Override
    public void onMetadata(RequestId requestId, String sqlId, String transactionStatus, List<Map<String, String>> columns) {
        this.sqlId = sqlId;
        this.columns = columns;
        delegate.onMetadata(requestId, sqlId, transactionStatus, columns);
    }

    @Override
    public void onRow(RequestId requestId, Map<String, Object> row) {
        rows.add(row);
        if (limit < 0 || delegatedCount < limit) {
            delegate.onRow(requestId, row);
            delegatedCount++;
        }
    }

    @Override
    public void onFooter(RequestId requestId, ExecutionStats stats) {
        if (sqlId != null) {
            onComplete.accept(new CachedResult(sqlId, columns, new ArrayList<>(rows), stats.executionTimeMs()));
        }
        
        // Enrich stats with pagination info if limited
        ExecutionStats enrichedStats = stats;
        if (limit > 0) {
            enrichedStats = ExecutionStats.success(
                delegatedCount, 
                stats.updateCount(), 
                stats.executionTimeMs(), 
                stats.rowCount(), // totalRows
                1,                // page
                limit             // pageSize
            );
        }
        delegate.onFooter(requestId, enrichedStats);
    }

    @Override
    public void onError(RequestId requestId, String code, String message) {
        delegate.onError(requestId, code, message);
    }
}
