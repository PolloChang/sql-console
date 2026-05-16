package work.pollochang.app.sqlconsole.protocol;

import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.RequestId;
import work.pollochang.app.sqlconsole.security.AuditLogger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Decorator for NdjsonStreamer that adds auditing for result streaming.
 */
public class AuditingNdjsonStreamer implements NdjsonStreamer {
    private final NdjsonStreamer delegate;
    private final AuditLogger auditLogger;
    private final String osUser;
    private final String dbUser;
    private final String profile;

    public AuditingNdjsonStreamer(NdjsonStreamer delegate, AuditLogger auditLogger, String osUser, String dbUser, String profile) {
        this.delegate = delegate;
        this.auditLogger = auditLogger;
        this.osUser = osUser;
        this.dbUser = dbUser;
        this.profile = profile;
    }

    @Override
    public void writeMessage(Message message) throws IOException {
        delegate.writeMessage(message);
    }

    @Override
    public void sendError(String requestId, String code, String message) {
        auditLogger.logSecurityViolation(osUser, "ERROR_SENT", 
            String.format("DB_User=%s, Profile=%s, Code=%s, Message=%s", dbUser, profile, code, message));
        delegate.sendError(requestId, code, message);
    }

    @Override
    public void onMetadata(RequestId requestId, String sqlId, String transactionStatus, List<Map<String, String>> columns) {
        delegate.onMetadata(requestId, sqlId, transactionStatus, columns);
    }

    @Override
    public void onRow(RequestId requestId, Map<String, Object> row) {
        delegate.onRow(requestId, row);
    }

    @Override
    public void onFooter(RequestId requestId, ExecutionStats stats) {
        auditLogger.logSecurityViolation(osUser, "QUERY_END", 
            String.format("DB_User=%s, Profile=%s, Rows=%d, Time=%dms", dbUser, profile, stats.rowCount(), stats.executionTimeMs()));
        delegate.onFooter(requestId, stats);
    }

    @Override
    public void onError(RequestId requestId, String code, String message) {
        auditLogger.logSecurityViolation(osUser, "QUERY_FAILURE", 
            String.format("DB_User=%s, Profile=%s, Code=%s, Message=%s", dbUser, profile, code, message));
        delegate.onError(requestId, code, message);
    }
}
