package work.pollochang.app.sqlconsole.engine;

import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.RequestId;

import java.util.List;
import java.util.Map;

/**
 * Interface for handling SQL execution results.
 * Decouples the execution engine from the output format (NDJSON, etc.).
 */
public interface ResultHandler {
    void onMetadata(RequestId requestId, String sqlId, String transactionStatus, List<Map<String, String>> columns);
    void onRow(RequestId requestId, Map<String, Object> row);
    void onFooter(RequestId requestId, ExecutionStats stats);
    void onError(RequestId requestId, String code, String message);
}
