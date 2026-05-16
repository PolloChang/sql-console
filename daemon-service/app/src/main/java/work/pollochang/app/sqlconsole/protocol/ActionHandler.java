package work.pollochang.app.sqlconsole.protocol;

import work.pollochang.app.sqlconsole.server.SessionContext;

/**
 * Interface for handling different protocol actions.
 */
public interface ActionHandler {
    void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer);
}
