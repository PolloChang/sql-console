package work.pollochang.app.sqlconsole.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientCommandProcessorTest {

    private ClientCommandProcessor processor;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @BeforeEach
    void setUp() {
        processor = new ClientCommandProcessor();
    }

    @Test
    void testTryRewrite_NonOracle() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        Optional<String> result = processor.tryRewrite(connection, "show parameter sga");

        assertFalse(result.isPresent());
    }

    @Test
    void testTryRewrite_Oracle_ShowParameter_NoKeyword() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle Database 19c Enterprise Edition");

        Optional<String> result = processor.tryRewrite(connection, "show parameter");

        assertTrue(result.isPresent());
        assertEquals("SELECT name, type, value, description FROM v$parameter ORDER BY name", result.get());
    }

    @Test
    void testTryRewrite_Oracle_ShowParameter_WithKeyword() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle Database 19c Enterprise Edition");

        Optional<String> result = processor.tryRewrite(connection, "  SHOW  PARAMETER  sga  ");

        assertTrue(result.isPresent());
        assertEquals("SELECT name, type, value, description FROM v$parameter WHERE name LIKE '%sga%' ORDER BY name", result.get());
    }

    @Test
    void testTryRewrite_Oracle_ShowParameter_WithSemicolon() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle Database 19c Enterprise Edition");

        Optional<String> result = processor.tryRewrite(connection, "show parameter sga;");

        assertTrue(result.isPresent());
        assertEquals("SELECT name, type, value, description FROM v$parameter WHERE name LIKE '%sga%' ORDER BY name", result.get());
    }

    @Test
    void testTryRewrite_Oracle_Desc_Simple() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle Database 19c Enterprise Edition");

        Optional<String> result = processor.tryRewrite(connection, "desc employees");

        assertTrue(result.isPresent());
        String expectedSql = "SELECT column_name AS \"Name\", " +
                "DECODE(nullable, 'N', 'NOT NULL', '') AS \"Null?\", " +
                "data_type || " +
                "CASE WHEN data_type IN ('VARCHAR2', 'CHAR') THEN '(' || data_length || ')' " +
                "     WHEN data_type = 'NUMBER' AND data_precision IS NOT NULL THEN '(' || data_precision || NVL2(data_scale, ',' || data_scale, '') || ')' " +
                "     ELSE '' END AS \"Type\" " +
                "FROM all_tab_columns " +
                "WHERE table_name = 'EMPLOYEES' " +
                "ORDER BY column_id";
        assertEquals(expectedSql, result.get());
    }

    @Test
    void testTryRewrite_Oracle_Desc_WithSemicolon() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle Database 19c Enterprise Edition");

        Optional<String> result = processor.tryRewrite(connection, "desc employees;");

        assertTrue(result.isPresent());
        String expectedSql = "SELECT column_name AS \"Name\", " +
                "DECODE(nullable, 'N', 'NOT NULL', '') AS \"Null?\", " +
                "data_type || " +
                "CASE WHEN data_type IN ('VARCHAR2', 'CHAR') THEN '(' || data_length || ')' " +
                "     WHEN data_type = 'NUMBER' AND data_precision IS NOT NULL THEN '(' || data_precision || NVL2(data_scale, ',' || data_scale, '') || ')' " +
                "     ELSE '' END AS \"Type\" " +
                "FROM all_tab_columns " +
                "WHERE table_name = 'EMPLOYEES' " +
                "ORDER BY column_id";
        assertEquals(expectedSql, result.get());
    }

    @Test
    void testTryRewrite_Oracle_Desc_WithSchema() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle Database 19c Enterprise Edition");

        Optional<String> result = processor.tryRewrite(connection, "describe HR.employees");

        assertTrue(result.isPresent());
        String expectedSql = "SELECT column_name AS \"Name\", " +
                "DECODE(nullable, 'N', 'NOT NULL', '') AS \"Null?\", " +
                "data_type || " +
                "CASE WHEN data_type IN ('VARCHAR2', 'CHAR') THEN '(' || data_length || ')' " +
                "     WHEN data_type = 'NUMBER' AND data_precision IS NOT NULL THEN '(' || data_precision || NVL2(data_scale, ',' || data_scale, '') || ')' " +
                "     ELSE '' END AS \"Type\" " +
                "FROM all_tab_columns " +
                "WHERE table_name = 'EMPLOYEES' " +
                "AND owner = 'HR' " +
                "ORDER BY column_id";
        assertEquals(expectedSql, result.get());
    }

    @Test
    void testTryRewrite_Security_SqlInjection_ShowParameter() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle Database 19c Enterprise Edition");

        // Single quote injection attempt should be blocked
        assertThrows(IllegalArgumentException.class, () -> {
            processor.tryRewrite(connection, "show parameter '; DROP TABLE users; --");
        });

        // Space and semicolon injection attempt should be blocked
        assertThrows(IllegalArgumentException.class, () -> {
            processor.tryRewrite(connection, "show parameter sga; drop table users");
        });
    }

    @Test
    void testTryRewrite_Security_SqlInjection_Desc() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle Database 19c Enterprise Edition");

        // SQL injection in table name should be blocked
        assertThrows(IllegalArgumentException.class, () -> {
            processor.tryRewrite(connection, "desc 'employees'; drop table users");
        });
    }
}
