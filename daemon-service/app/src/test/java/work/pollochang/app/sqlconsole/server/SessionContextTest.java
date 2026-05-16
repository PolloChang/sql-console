package work.pollochang.app.sqlconsole.server;

import com.zaxxer.hikari.HikariDataSource;
import work.pollochang.app.sqlconsole.domain.JdbcProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionContextTest {

    private SessionContext sessionContext;

    @Mock
    private HikariDataSource dataSource;

    @Mock
    private Connection connection;

    @BeforeEach
    void setUp() {
        sessionContext = new SessionContext("test-session");
    }

    @Test
    void testGetConnection() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        sessionContext.setProfile(new JdbcProfile("test", "jdbc:h2:mem:test", "user", "pass"), dataSource);

        Connection conn = sessionContext.getConnection();
        
        assertNotNull(conn);
        assertEquals(connection, conn);
        verify(connection).setAutoCommit(true);
    }

    @Test
    void testDisconnectRollbackWhenNoAutoCommit() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        sessionContext.setProfile(new JdbcProfile("test", "jdbc:h2:mem:test", "user", "pass"), dataSource);
        sessionContext.setAutoCommit(false);
        
        Connection conn = sessionContext.getConnection();
        when(conn.isClosed()).thenReturn(false);
        
        sessionContext.disconnect();
        
        verify(connection).rollback();
        verify(connection).close();
        assertThrows(SQLException.class, () -> sessionContext.getConnection());
    }
}
