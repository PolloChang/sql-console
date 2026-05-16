package work.pollochang.app.sqlconsole.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.RequestId;
import work.pollochang.app.sqlconsole.engine.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ResultHandler that streams results as NDJSON over a SocketChannel.
 */
public class SocketNdjsonStreamer implements NdjsonStreamer {
    private static final Logger logger = LoggerFactory.getLogger(SocketNdjsonStreamer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SocketChannel channel;

    public SocketNdjsonStreamer(SocketChannel channel) {
        this.channel = channel;
    }

    /**
     * Writes a Message object as a single line of JSON followed by a newline.
     */
    public void writeMessage(Message message) throws IOException {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(message);
        ByteBuffer buffer = ByteBuffer.allocate(jsonBytes.length + 1);
        buffer.put(jsonBytes);
        buffer.put((byte) '\n');
        buffer.flip();

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    @Override
    public void onMetadata(RequestId requestId, String sqlId, String transactionStatus, List<Map<String, String>> columns) {
        try {
            writeMessage(Message.header(requestId.id(), Map.of(
                "sqlId", sqlId != null ? sqlId : "",
                "transaction", transactionStatus != null ? transactionStatus : "auto-commit",
                "columns", columns
            )));
        } catch (IOException e) {
            logger.error("Failed to send header", e);
        }
    }

    @Override
    public void onRow(RequestId requestId, Map<String, Object> row) {
        try {
            writeMessage(Message.row(requestId.id(), row));
        } catch (IOException e) {
            logger.error("Failed to send row", e);
        }
    }

    @Override
    public void onFooter(RequestId requestId, ExecutionStats stats) {
        try {
            // Convert ExecutionStats to a map for the message payload
            // Use LinkedHashMap to preserve order if needed, but here simple Map.of is fine if it fits
            Map<String, Object> payload = Map.of(
                "rowCount", stats.rowCount(),
                "updateCount", stats.updateCount(),
                "executionTimeMs", stats.executionTimeMs(),
                "status", stats.status(),
                "totalRows", stats.totalRows(),
                "page", stats.page(),
                "pageSize", stats.pageSize()
            );
            writeMessage(Message.footer(requestId.id(), payload));
        } catch (IOException e) {
            logger.error("Failed to send footer", e);
        }
    }

    @Override
    public void onError(RequestId requestId, String code, String message) {
        sendError(requestId.id(), code, message);
    }

    /**
     * Helper to send an error message.
     */
    public void sendError(String requestId, String code, String message) {
        try {
            writeMessage(Message.error(requestId, new ErrorPayload(code, message, null)));
        } catch (IOException e) {
            logger.error("Failed to send error message", e);
        }
    }

    public record ErrorPayload(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("stackTrace") String stackTrace
    ) {}
}
