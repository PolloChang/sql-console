package work.pollochang.app.sqlconsole.engine;

import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.RequestId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles SQL execution and streams results through a ResultHandler.
 */
public class QueryExecutor {
    private static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);

    public void execute(Connection conn, String sql, String sqlId, String transactionStatus, RequestId requestId, ResultHandler handler) {
        logger.info("Executing SQL: [{}]", sql);
        long startTime = System.currentTimeMillis();
        try (Statement stmt = conn.createStatement()) {
            boolean isResultSet = stmt.execute(sql);
            
            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // 1. Send Metadata
                    List<Map<String, String>> columns = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Map<String, String> col = new LinkedHashMap<>();
                        col.put("name", metaData.getColumnName(i));
                        col.put("type", metaData.getColumnTypeName(i));
                        columns.add(col);
                    }
                    handler.onMetadata(requestId, sqlId, transactionStatus, columns);
                    
                    // 2. Stream Rows
                    long rowCount = 0;
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            Object value = rs.getObject(i);
                            row.put(metaData.getColumnName(i), work.pollochang.app.sqlconsole.util.JdbcTypeConverter.convert(value));
                        }
                        handler.onRow(requestId, row);
                        rowCount++;
                    }
                    
                    // 3. Send Footer
                    long duration = System.currentTimeMillis() - startTime;
                    handler.onFooter(requestId, ExecutionStats.success(rowCount, 0, duration));
                }
            } else {
                // UPDATE / INSERT / DELETE
                int updateCount = stmt.getUpdateCount();
                long duration = System.currentTimeMillis() - startTime;
                handler.onFooter(requestId, ExecutionStats.success(0, updateCount, duration));
            }
        } catch (SQLException e) {
            logger.error("SQL Execution failed: " + sql, e);
            handler.onError(requestId, "SQL-" + e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during SQL execution", e);
            handler.onError(requestId, "SYS-500", e.getMessage());
        }
    }
}
