package work.pollochang.app.sqlconsole.server;

import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.ProtocolHandler;
import work.pollochang.app.sqlconsole.security.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Unix Domain Socket Server.
 * Listens for connections and dispatches them to ProtocolHandler.
 */
public class UdsServer {
    private static final Logger logger = LoggerFactory.getLogger(UdsServer.class);
    private final Path socketPath;
    private final Map<String, ActionHandler> actionHandlers;
    private final AuditLogger auditLogger;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public UdsServer(Path socketPath, Map<String, ActionHandler> actionHandlers, AuditLogger auditLogger) {
        this.socketPath = socketPath;
        this.actionHandlers = actionHandlers;
        this.auditLogger = auditLogger;
        // RFP says max 50 concurrent sessions
        this.executorService = Executors.newFixedThreadPool(50);
    }

    public void start() throws IOException {
        if (Files.exists(socketPath)) {
            logger.info("Cleaning up existing socket file: {}", socketPath);
            Files.delete(socketPath);
        }

        // Ensure parent directory exists
        if (socketPath.getParent() != null) {
            Files.createDirectories(socketPath.getParent());
        }

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
            serverChannel.bind(address);
            
            // Set permissions: rw-rw-rw- (666)
            try {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-rw-rw-");
                Files.setPosixFilePermissions(socketPath, perms);
                logger.info("Set socket permissions to 666: {}", socketPath);
            } catch (UnsupportedOperationException e) {
                logger.warn("POSIX file permissions not supported on this filesystem: {}", socketPath);
            }

            logger.info("UDS Server started at {}", socketPath);
            auditLogger.logServerStart(socketPath.toString());

            while (running) {
                SocketChannel clientChannel = serverChannel.accept();
                logger.info("Accepted new connection from {}", clientChannel);
                executorService.submit(() -> handleClient(clientChannel));
            }
        } catch (IOException e) {
            if (running) {
                logger.error("UDS Server error", e);
                throw e;
            }
        }
    }

    private void handleClient(SocketChannel clientChannel) {
        try (clientChannel) {
            logger.info("Handling client connection: {}", clientChannel);
            ProtocolHandler handler = new ProtocolHandler(clientChannel, actionHandlers);
            handler.handleSession();
        } catch (Exception e) {
            logger.error("Error handling client", e);
        }
    }

    public void stop() {
        running = false;
        auditLogger.logServerStop();
        executorService.shutdownNow();
    }
}
