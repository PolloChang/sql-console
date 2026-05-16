package work.pollochang.app.sqlconsole.protocol.handler;

import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.RequestId;
import work.pollochang.app.sqlconsole.engine.CachedResult;
import work.pollochang.app.sqlconsole.engine.SqlEngine;
import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles 'fetch' action for pagination.
 * Retrieves data from SqlEngine's queryCache.
 */
public class FetchHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(FetchHandler.class);
    private final SqlEngine sqlEngine;
    private static final int PAGE_SIZE = 20;

    public FetchHandler(SqlEngine sqlEngine) {
        this.sqlEngine = sqlEngine;
    }

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) request.payload();
            String sqlId = (String) payload.get("sqlId");
            int page = (int) payload.getOrDefault("page", 1);
            
            CachedResult result = sqlEngine.getCachedResult(sqlId);
            if (result == null) {
                streamer.sendError(request.requestId(), "FCH-001", "Cached result not found for SQL ID: " + sqlId);
                return;
            }

            RequestId rid = RequestId.of(request.requestId());
            String txStatus = sessionContext.isAutoCommit() ? "auto-commit" : "manual-commit";
            
            // Send header
            streamer.onMetadata(rid, sqlId, txStatus, result.columns());

            // Determine range
            int totalRows = result.rows().size();
            int start = (page - 1) * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, totalRows);

            if (start < totalRows) {
                for (int i = start; i < end; i++) {
                    streamer.onRow(rid, result.rows().get(i));
                }
            }

            // Send footer
            streamer.onFooter(rid, ExecutionStats.success(
                end - start, 
                0, 
                0, 
                totalRows, 
                page, 
                PAGE_SIZE
            ));
            
        } catch (Exception e) {
            logger.error("Failed to fetch page", e);
            streamer.sendError(request.requestId(), "FCH-002", "Failed to fetch page: " + e.getMessage());
        }
    }
}
