package work.pollochang.app.sqlconsole.protocol.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.ImportRequest;
import work.pollochang.app.sqlconsole.domain.RequestId;
import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles 'import' action. Batched CSV data insertion with precise error handling.
 */
public class ImportHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ImportHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        try {
            ImportRequest importReq = objectMapper.convertValue(request.payload(), ImportRequest.class);
            if (importReq == null || importReq.table() == null || importReq.table().isBlank() || importReq.columns() == null || importReq.columns().isEmpty() || importReq.rows() == null) {
                streamer.sendError(request.requestId(), "IMP-001", "Invalid import payload: missing table name, columns, or rows");
                return;
            }

            String tableName = importReq.table();
            List<String> columns = importReq.columns();
            List<List<Object>> rows = importReq.rows();

            Connection conn = sessionContext.getConnection();
            if (conn == null) {
                streamer.sendError(request.requestId(), "DB-003", "No active database connection in session");
                return;
            }

            if (rows.isEmpty()) {
                streamer.onFooter(RequestId.of(request.requestId()), ExecutionStats.success(0, 0, 0));
                return;
            }

            // Build INSERT statement
            StringBuilder sql = new StringBuilder("INSERT INTO ");
            sql.append(tableName).append(" (");
            sql.append(String.join(", ", columns));
            sql.append(") VALUES (");
            String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
            sql.append(placeholders).append(")");

            long startTime = System.currentTimeMillis();
            int totalInserted = 0;

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (List<Object> row : rows) {
                    for (int i = 0; i < columns.size(); i++) {
                        Object val = row.get(i);
                        if (val == null || (val instanceof String s && s.isEmpty())) {
                            pstmt.setObject(i + 1, null);
                        } else {
                            pstmt.setObject(i + 1, val);
                        }
                    }
                    pstmt.addBatch();
                }

                int[] counts = pstmt.executeBatch();
                for (int c : counts) {
                    if (c >= 0) totalInserted += c;
                    else if (c == Statement.SUCCESS_NO_INFO) totalInserted += 1;
                }
            } catch (BatchUpdateException be) {
                int[] updateCounts = be.getUpdateCounts();
                int successfulBeforeFailure = 0;
                if (updateCounts != null) {
                    for (int c : updateCounts) {
                        if (c >= 0) successfulBeforeFailure += c;
                        else if (c == Statement.SUCCESS_NO_INFO) successfulBeforeFailure += 1;
                    }
                }
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Batch update failed after inserting {} rows: {}", successfulBeforeFailure, be.getMessage(), be);
                streamer.sendError(request.requestId(), "IMP-500", "Batch update failed at row " + (successfulBeforeFailure + 1) + ": " + be.getMessage());
                return;
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully imported {} rows into table {} in {}ms", totalInserted, tableName, duration);

            streamer.onFooter(RequestId.of(request.requestId()), ExecutionStats.success(0, totalInserted, duration));

        } catch (Exception e) {
            logger.error("Import failed", e);
            streamer.sendError(request.requestId(), "IMP-500", "Import failed: " + e.getMessage());
        }
    }
}
