package work.pollochang.app.sqlconsole.protocol.handler;

import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.RequestId;
import work.pollochang.app.sqlconsole.protocol.ActionHandler;
import work.pollochang.app.sqlconsole.protocol.NdjsonStreamer;
import work.pollochang.app.sqlconsole.protocol.Request;
import work.pollochang.app.sqlconsole.server.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles 'list-tables' action.
 * Uses JDBC DatabaseMetaData to fetch table names and schemas.
 */
public class ListTablesHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ListTablesHandler.class);

    @Override
    public void handle(Request request, SessionContext sessionContext, NdjsonStreamer streamer) {
        RequestId rid = RequestId.of(request.requestId());
        long startTime = System.currentTimeMillis();
        int rowCount = 0;

        try {
            Connection conn = sessionContext.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Define metadata columns (using names that match common JDBC metadata or simple names)
            List<Map<String, String>> columns = List.of(
                Map.of("name", "schema", "type", "STRING"),
                Map.of("name", "name", "type", "STRING"),
                Map.of("name", "type", "type", "STRING"),
                Map.of("name", "remarks", "type", "STRING")
            );
            String txStatus = sessionContext.isAutoCommit() ? "auto-commit" : "manual-commit";
            streamer.onMetadata(rid, "list-tables", txStatus, columns);

            // Fetch tables and views
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    String schema = rs.getString("TABLE_SCHEM");
                    String name = rs.getString("TABLE_NAME");
                    String type = rs.getString("TABLE_TYPE");
                    String remarks = rs.getString("REMARKS");

                    row.put("schema", schema != null ? schema : "");
                    row.put("name", name != null ? name : "");
                    row.put("type", type != null ? type : "");
                    row.put("remarks", remarks != null ? remarks : "");

                    streamer.onRow(rid, row);
                    rowCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            streamer.onFooter(rid, ExecutionStats.success(rowCount, 0, duration));

        } catch (Exception e) {
            logger.error("Failed to list tables", e);
            streamer.sendError(request.requestId(), "ACT-002", "Failed to list tables: " + e.getMessage());
        }
    }
}
