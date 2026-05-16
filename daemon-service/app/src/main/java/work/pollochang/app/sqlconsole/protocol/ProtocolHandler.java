package work.pollochang.app.sqlconsole.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import work.pollochang.app.sqlconsole.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the communication protocol over a SocketChannel.
 * Orchestrates request parsing, action dispatching, and session management.
 */
public class ProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolHandler.class);
    private final SocketChannel channel;
    private final Map<String, ActionHandler> actionHandlers;
    private final NdjsonStreamer streamer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionContext sessionContext;
    private final StringBuilder inputBuffer = new StringBuilder();

    public ProtocolHandler(SocketChannel channel, Map<String, ActionHandler> actionHandlers) {
        this.channel = channel;
        this.actionHandlers = actionHandlers;
        this.streamer = new SocketNdjsonStreamer(channel);
        this.sessionContext = new SessionContext(UUID.randomUUID().toString());
    }

    /**
     * Entry point for session handling. Reads from channel until disconnected.
     */
    public void handleSession() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            while (channel.isOpen()) {
                buffer.clear();
                int bytesRead = channel.read(buffer);
                if (bytesRead == -1) {
                    logger.info("Client disconnected: {}", channel);
                    break;
                }
                
                buffer.flip();
                String chunk = StandardCharsets.UTF_8.decode(buffer).toString();
                processInput(chunk);
            }
        } catch (IOException e) {
            logger.error("IO error in session handling", e);
        } finally {
            cleanup();
        }
    }

    private void processInput(String chunk) {
        inputBuffer.append(chunk);
        int newlineIndex;
        while ((newlineIndex = inputBuffer.indexOf("\n")) != -1) {
            String line = inputBuffer.substring(0, newlineIndex).trim();
            inputBuffer.delete(0, newlineIndex + 1);
            if (!line.isEmpty()) {
                handleRequest(line);
            }
        }
    }

    private void handleRequest(String line) {
        try {
            Request request = objectMapper.readValue(line, Request.class);
            logger.info("Received request: {}", request);

            // Capture OS User for auditing
            if (request.osUser() != null && !request.osUser().isEmpty()) {
                sessionContext.setOsUser(request.osUser());
            }

            // Protocol Version Check
            if (!"1.0".equals(request.version())) {
                streamer.sendError(request.requestId(), "VER-001", "Incompatible protocol version: " + request.version());
                return;
            }

            // Dispatch to ActionHandler
            ActionHandler handler = actionHandlers.get(request.action());
            if (handler != null) {
                handler.handle(request, sessionContext, streamer);
            } else {
                streamer.sendError(request.requestId(), "ACT-001", "Unknown action: " + request.action());
            }

        } catch (Exception e) {
            logger.error("Error processing request", e);
            streamer.sendError("unknown", "SYS-001", "Internal server error: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            sessionContext.disconnect();
        } catch (Exception e) {
            logger.error("Error disconnecting session", e);
        }
        closeChannel();
    }

    private void closeChannel() {
        try {
            if (channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            logger.error("Error closing channel", e);
        }
    }
}
