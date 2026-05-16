package work.pollochang.app.sqlconsole.protocol.handler;

import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.security.AuditLogger;
import work.pollochang.app.sqlconsole.server.SessionContext;

import java.util.Map;

/**
 * Decorator specifically for ConnectHandler to audit connection attempts.
 */
public class AuditedConnectHandler implements ActionHandler {
    private final ActionHandler delegate;
    private final AuditLogger auditLogger;

    public AuditedConnectHandler(ActionHandler delegate, AuditLogger auditLogger) {
        this.delegate = delegate;
        this.auditLogger = auditLogger;
    }

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        @SuppressWarnings("unchecked")
        Map<String, String> payload = (Map<String, String>) request.payload();
        String profileName = payload.get("profile");
        String username = payload.get("username");
        String osUser = sessionContext.getOsUser();

        auditLogger.logConnectionAttempt(osUser, profileName, username, "UDS");

        try {
            delegate.handle(request, sessionContext, streamer);
            
            // After successful delegation
            if (sessionContext.getProfile() != null) {
                auditLogger.logConnectionSuccess(osUser, profileName, username);
            }
        } catch (Exception e) {
            auditLogger.logConnectionFailure(osUser, profileName, username, e.getMessage());
            throw e; // Rethrow to let the caller handle it if necessary, though handlers usually catch internally
        }
    }
}
