package work.pollochang.app.sqlconsole.protocol.handler;

import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.Message;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles 'set-transaction' action.
 */
public class SetTransactionHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(SetTransactionHandler.class);

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) request.payload();
            String mode = (String) payload.get("mode");
            
            boolean autoCommit = !"manual".equalsIgnoreCase(mode) && !"manual-commit".equalsIgnoreCase(mode);
            sessionContext.setAutoCommit(autoCommit);
            
            logger.info("Transaction mode set to {} for session {}", mode, sessionContext.getSessionId());
            
            // Send success message (Go client expects success or footer to finish)
            streamer.writeMessage(Message.success(request.requestId(), 
                Map.of("status", "SUCCESS", "message", "Transaction mode set to " + (autoCommit ? "auto-commit" : "manual-commit"))));
                
        } catch (Exception e) {
            logger.error("Failed to set transaction mode", e);
            streamer.sendError(request.requestId(), "TX-001", "Failed to set transaction mode: " + e.getMessage());
        }
    }
}
