package work.pollochang.app.sqlconsole.protocol.handler;

import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.Message;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Handles 'ping' action.
 */
public class PingHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(PingHandler.class);

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        try {
            streamer.writeMessage(Message.footer(request.requestId(), 
                Map.of("status", "pong", "sessionId", sessionContext.getSessionId())));
        } catch (IOException e) {
            logger.error("Failed to send ping response", e);
        }
    }
}
