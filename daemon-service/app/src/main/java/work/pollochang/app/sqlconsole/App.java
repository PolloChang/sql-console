package work.pollochang.app.sqlconsole;

import work.pollochang.app.sqlconsole.db.ConnectionManager;
import work.pollochang.app.sqlconsole.db.ConnectionPoolManager;
import work.pollochang.app.sqlconsole.db.JdbcDriverLoader;
import work.pollochang.app.sqlconsole.engine.SqlEngine;
import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.handler.*;
import work.pollochang.app.sqlconsole.security.AuditLogger;
import work.pollochang.app.sqlconsole.server.UdsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application entry point.
 * Performs manual dependency injection and starts the UDS server.
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("Starting sql-console Daemon...");
        
        // 1. Initialize Core Services
        // Note: In a larger app, use a proper DI framework like Spring or Guice.
        // For this project, manual DI is sufficient and addresses the static coupling issue.
        JdbcDriverLoader driverLoader = new JdbcDriverLoader(Paths.get("libs"));
        driverLoader.loadDrivers();

        ConnectionPoolManager poolManager = new ConnectionPoolManager();
        ConnectionManager connectionManager = new ConnectionManager(poolManager);
        SqlEngine sqlEngine = new SqlEngine();
        AuditLogger auditLogger = new AuditLogger();

        // 2. Initialize Action Handlers (Command Pattern with Decorator)
        Map<String, ActionHandler> handlers = new HashMap<>();
        
        // Base Handlers
        ActionHandler pingHandler = new PingHandler();
        ActionHandler connectHandler = new ConnectHandler(poolManager, connectionManager);
        ActionHandler queryHandler = new QueryHandler(sqlEngine);
        ActionHandler listTablesHandler = new ListTablesHandler();
        ActionHandler setTransactionHandler = new SetTransactionHandler();
        ActionHandler fetchHandler = new FetchHandler(sqlEngine);

        // Wrap with Auditing Decorators
        handlers.put("ping", new AuditedActionHandler(pingHandler, auditLogger));
        handlers.put("connect", new AuditedConnectHandler(connectHandler, auditLogger));
        handlers.put("query", new AuditedQueryHandler(queryHandler, auditLogger));
        handlers.put("list-tables", new AuditedActionHandler(listTablesHandler, auditLogger));
        handlers.put("set-transaction", new AuditedActionHandler(setTransactionHandler, auditLogger));
        handlers.put("fetch", new AuditedActionHandler(fetchHandler, auditLogger));

        // 3. Start UDS Server
        String socketPathStr = System.getProperty("sql.console.sock", "/run/sql-console/sql-console.sock");
        Path socketPath = Paths.get(socketPathStr);
        UdsServer server = new UdsServer(socketPath, handlers, auditLogger);

        // Shutdown Hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered. Cleaning up...");
            server.stop();
            poolManager.shutdownAll();
        }));

        try {
            server.start();
        } catch (Exception e) {
            logger.error("Failed to start UDS Server", e);
            System.exit(1);
        }
    }
}
