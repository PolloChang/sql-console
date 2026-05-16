package work.pollochang.app.sqlconsole.protocol.handler;

import work.pollochang.app.sqlconsole.domain.RequestId;
import work.pollochang.app.sqlconsole.engine.SqlEngine;
import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles 'query' action. Pure business logic.
 */
public class QueryHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(QueryHandler.class);
    private final SqlEngine sqlEngine;

    public QueryHandler(SqlEngine sqlEngine) {
        this.sqlEngine = sqlEngine;
    }

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) request.payload();
            String sql = (String) payload.get("sql");
            int pageSize = -1;
            if (payload.containsKey("pageSize")) {
                pageSize = ((Number) payload.get("pageSize")).intValue();
            }
            
            sqlEngine.executeBatch(sessionContext, sql, RequestId.of(request.requestId()), streamer, pageSize);
        } catch (Exception e) {
            logger.error("Query failed", e);
            streamer.sendError(request.requestId(), "QRY-001", "Query failed: " + e.getMessage());
        }
    }
}
