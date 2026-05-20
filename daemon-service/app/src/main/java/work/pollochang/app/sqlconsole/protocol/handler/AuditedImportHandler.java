package work.pollochang.app.sqlconsole.protocol.handler;

import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.AuditingNdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.security.AuditLogger;
import work.pollochang.app.sqlconsole.server.SessionContext;

import java.util.List;
import java.util.Map;

/**
 * Decorator specifically for ImportHandler to audit data import operations.
 */
public class AuditedImportHandler implements ActionHandler {
    private final ActionHandler delegate;
    private final AuditLogger auditLogger;

    public AuditedImportHandler(ActionHandler delegate, AuditLogger auditLogger) {
        this.delegate = delegate;
        this.auditLogger = auditLogger;
    }

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) request.payload();
        String tableName = (String) payload.get("table");
        @SuppressWarnings("unchecked")
        List<List<Object>> rows = (List<List<Object>>) payload.get("rows");
        int rowCount = rows != null ? rows.size() : 0;

        String dbUser = sessionContext.getProfile() != null ? sessionContext.getProfile().username().value() : "unknown";
        String osUser = sessionContext.getOsUser();
        String profileName = sessionContext.getProfile() != null ? sessionContext.getProfile().profileName().value() : "none";

        String simulatedSql = String.format("IMPORT INTO %s (%d rows)", tableName, rowCount);
        auditLogger.logQueryStart(osUser, profileName, dbUser, simulatedSql);

        AuditingNdjsonStreamer auditingStreamer = new AuditingNdjsonStreamer(streamer, auditLogger, osUser, dbUser, profileName);
        delegate.handle(request, sessionContext, auditingStreamer);
    }
}
