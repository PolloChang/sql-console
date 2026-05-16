package work.pollochang.app.sqlconsole.protocol;

import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.RequestId;
import work.pollochang.app.sqlconsole.engine.ResultHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for streaming NDJSON messages over a protocol.
 * Extends ResultHandler to handle SQL execution results.
 */
public interface NdjsonStreamer extends ResultHandler {
    void writeMessage(Message message) throws IOException;
    void sendError(String requestId, String code, String message);
    
    // ResultHandler methods are inherited
    @Override
    void onMetadata(RequestId requestId, String sqlId, String transactionStatus, List<Map<String, String>> columns);
    @Override
    void onRow(RequestId requestId, Map<String, Object> row);
    @Override
    void onFooter(RequestId requestId, ExecutionStats stats);
    @Override
    void onError(RequestId requestId, String code, String message);
}
