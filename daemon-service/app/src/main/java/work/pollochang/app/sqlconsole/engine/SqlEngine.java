package work.pollochang.app.sqlconsole.engine;

import work.pollochang.app.sqlconsole.domain.RequestId;
import work.pollochang.app.sqlconsole.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL execution core engine.
 * Decoupled from the output format by using ResultHandler.
 */
public class SqlEngine {
    private static final Logger logger = LoggerFactory.getLogger(SqlEngine.class);
    private final SqlSplitter splitter = new SqlSplitter();
    private final QueryExecutor executor = new QueryExecutor();
    private final ClientCommandProcessor commandProcessor = new ClientCommandProcessor();
    
    private final int MAX_CACHE_SIZE = 100;
    private final Map<String, Object> queryCache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    /**
     * Executes a batch of SQL statements.
     */
    public void executeBatch(SessionContext session, String rawSql, RequestId requestId, ResultHandler handler, int limit) {
        List<String> statements = splitter.split(rawSql);
        logger.info("Executing batch of {} statements for session {}", statements.size(), session.getSessionId());

        try {
            Connection conn = session.getConnection();
            for (String sql : statements) {
                if (sql.isBlank()) continue;
                
                String sqlId = generateSqlId(sql);
                String txStatus = session.isAutoCommit() ? "auto-commit" : "manual-commit";
                logger.debug("Executing SQL [{}]: {}", sqlId, sql);
                
                // Wrap handler with CachingResultHandler
                ResultHandler cachingHandler = new CachingResultHandler(handler, result -> {
                    queryCache.put(result.sqlId(), result);
                }, limit);
                
                String sqlToExecute = sql;
                try {
                    java.util.Optional<String> rewrittenSql = commandProcessor.tryRewrite(conn, sql);
                    if (rewrittenSql.isPresent()) {
                        sqlToExecute = rewrittenSql.get();
                        logger.info("SQL [{}] intercepted and rewritten to: [{}]", sqlId, sqlToExecute);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Client command validation failed: {}", e.getMessage());
                    cachingHandler.onError(requestId, "SEC-403", e.getMessage());
                    continue;
                }
                
                executor.execute(conn, sqlToExecute, sqlId, txStatus, requestId, cachingHandler);
            }
        } catch (Exception e) {
            logger.error("Failed to execute batch", e);
            handler.onError(requestId, "DB-002", "Failed to execute: " + e.getMessage());
        }
    }

    public CachedResult getCachedResult(String sqlId) {
        return (CachedResult) queryCache.get(sqlId);
    }

    private String generateSqlId(String sql) {
        return Integer.toHexString(sql.trim().toLowerCase().hashCode());
    }
}
