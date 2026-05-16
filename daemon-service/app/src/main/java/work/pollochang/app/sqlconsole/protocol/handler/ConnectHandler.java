package work.pollochang.app.sqlconsole.protocol.handler;

import com.zaxxer.hikari.HikariDataSource;
import work.pollochang.app.sqlconsole.db.ConnectionManager;
import work.pollochang.app.sqlconsole.db.ConnectionPoolManager;
import work.pollochang.app.sqlconsole.domain.JdbcProfile;
import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.Message;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles 'connect' action.
 */
public class ConnectHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectHandler.class);
    private final ConnectionPoolManager poolManager;
    private final ConnectionManager connectionManager;

    public ConnectHandler(ConnectionPoolManager poolManager, ConnectionManager connectionManager) {
        this.poolManager = poolManager;
        this.connectionManager = connectionManager;
    }

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = (Map<String, String>) request.payload();
            String profileName = payload.get("profile");
            String url = payload.get("url");
            String username = payload.get("username");
            String password = payload.get("password");

            JdbcProfile profile = new JdbcProfile(profileName, url, username, password);
            
            // Validate connection
            connectionManager.validateProfile(profile);
            
            // Set in session
            HikariDataSource ds = poolManager.getOrCreatePool(profile);
            sessionContext.setProfile(profile, ds);
            
            streamer.writeMessage(Message.footer(request.requestId(), 
                Map.of("status", "SUCCESS", "message", "Connected to " + profileName)));
            
        } catch (Exception e) {
            logger.error("Connection failed", e);
            streamer.sendError(request.requestId(), "DB-001", "Connection failed: " + e.getMessage());
        }
    }
}
