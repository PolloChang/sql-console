package work.pollochang.app.sqlconsole.engine;

import work.pollochang.app.sqlconsole.domain.ExecutionStats;
import work.pollochang.app.sqlconsole.domain.RequestId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueryExecutorTest {

    private QueryExecutor queryExecutor;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData metaData;

    @Mock
    private ResultHandler resultHandler;

    private final RequestId requestId = RequestId.of("test-req");

    @BeforeEach
    void setUp() throws SQLException {
        queryExecutor = new QueryExecutor();
        when(connection.createStatement()).thenReturn(statement);
    }

    @Test
    void testExecuteQuery() throws SQLException {
        String sql = "SELECT * FROM users";
        when(statement.execute(sql)).thenReturn(true);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnTypeName(1)).thenReturn("INTEGER");
        
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(1);

        queryExecutor.execute(connection, sql, "test-sql-id", "auto-commit", requestId, resultHandler);

        verify(resultHandler).onMetadata(eq(requestId), eq("test-sql-id"), eq("auto-commit"), any(List.class));
        verify(resultHandler).onRow(eq(requestId), any(Map.class));
        verify(resultHandler).onFooter(eq(requestId), any(ExecutionStats.class));
    }

    @Test
    void testExecuteUpdate() throws SQLException {
        String sql = "UPDATE users SET name = 'test'";
        when(statement.execute(sql)).thenReturn(false);
        when(statement.getUpdateCount()).thenReturn(1);

        queryExecutor.execute(connection, sql, "test-sql-id", "auto-commit", requestId, resultHandler);

        verify(resultHandler, never()).onMetadata(any(), any(), any(), any());
        verify(resultHandler).onFooter(eq(requestId), argThat(stats -> stats.updateCount() == 1));
    }
}
