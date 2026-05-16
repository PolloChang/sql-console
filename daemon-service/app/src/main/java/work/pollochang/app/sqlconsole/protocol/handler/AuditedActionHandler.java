package work.pollochang.app.sqlconsole.protocol.handler;

import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.security.AuditLogger;
import work.pollochang.app.sqlconsole.server.SessionContext;

/**
 * Decorator for ActionHandler that adds generic request auditing.
 */
public class AuditedActionHandler implements ActionHandler {
    private final ActionHandler delegate;
    private final AuditLogger auditLogger;

    public AuditedActionHandler(ActionHandler delegate, AuditLogger auditLogger) {
        this.delegate = delegate;
        this.auditLogger = auditLogger;
    }

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        String dbUser = sessionContext.getProfile() != null ? sessionContext.getProfile().username().value() : "unknown";
        String osUser = sessionContext.getOsUser();
        String profile = sessionContext.getProfile() != null ? sessionContext.getProfile().profileName().value() : "none";
        
        // General audit for any action
        if (!"query".equals(request.action()) && !"connect".equals(request.action())) {
            auditLogger.logSecurityViolation(osUser, "REQUEST_HANDLING", 
                String.format("Action=%s, DB_User=%s, Profile=%s", request.action(), dbUser, profile));
        }

        delegate.handle(request, sessionContext, streamer);
    }
}
