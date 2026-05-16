package work.pollochang.app.sqlconsole.protocol.handler;

import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.AuditingNdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.security.AuditLogger;
import work.pollochang.app.sqlconsole.server.SessionContext;

import java.util.Map;

/**
 * Decorator specifically for QueryHandler to audit SQL execution.
 */
public class AuditedQueryHandler implements ActionHandler {
    private final ActionHandler delegate;
    private final AuditLogger auditLogger;

    public AuditedQueryHandler(ActionHandler delegate, AuditLogger auditLogger) {
        this.delegate = delegate;
        this.auditLogger = auditLogger;
    }

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) request.payload();
        String sql = (String) payload.get("sql");
        
        String dbUser = sessionContext.getProfile() != null ? sessionContext.getProfile().username().value() : "unknown";
        String osUser = sessionContext.getOsUser();
        String profileName = sessionContext.getProfile() != null ? sessionContext.getProfile().profileName().value() : "none";
        
        auditLogger.logQueryStart(osUser, profileName, dbUser, sql);

        // Wrap the streamer with AuditingNdjsonStreamer
        AuditingNdjsonStreamer auditingStreamer = new AuditingNdjsonStreamer(streamer, auditLogger, osUser, dbUser, profileName);
        
        delegate.handle(request, sessionContext, auditingStreamer);
    }
}
