package work.pollochang.app.sqlconsole.server;

import com.zaxxer.hikari.HikariDataSource;
import work.pollochang.app.sqlconsole.domain.JdbcProfile;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * State holder for a session.
 */
public class SessionContext {
    private final String sessionId;
    private JdbcProfile profile;
    private HikariDataSource dataSource;
    private Connection currentConnection;
    private String osUser;
    private boolean autoCommit = true;

    public SessionContext(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setProfile(JdbcProfile profile, HikariDataSource dataSource) {
        this.profile = profile;
        this.dataSource = dataSource;
    }

    public JdbcProfile getProfile() {
        return profile;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public void setCurrentConnection(Connection connection) {
        this.currentConnection = connection;
    }

    public Connection getCurrentConnection() throws SQLException {
        if (currentConnection != null && currentConnection.isClosed()) {
            currentConnection = null;
        }
        return currentConnection;
    }

    public void disconnect() throws SQLException {
        if (currentConnection != null && !currentConnection.isClosed()) {
            if (!autoCommit) {
                currentConnection.rollback();
            }
            currentConnection.close();
        }
        currentConnection = null;
        dataSource = null;
        profile = null;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autoCommit = autoCommit;
        if (currentConnection != null && !currentConnection.isClosed()) {
            currentConnection.setAutoCommit(autoCommit);
        }
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setOsUser(String osUser) {
        this.osUser = osUser;
    }

    public String getOsUser() {
        return osUser != null ? osUser : "unknown";
    }

    /**
     * Helper to get connection, but logic should be moved to a manager if possible.
     * Keeping it here for now to avoid breaking too much, but it's a "stateful" getter.
     */
    public Connection getConnection() throws SQLException {
        Connection conn = getCurrentConnection();
        if (conn == null) {
            if (dataSource == null) {
                throw new SQLException("No database profile connected for this session.");
            }
            conn = dataSource.getConnection();
            conn.setAutoCommit(autoCommit);
            setCurrentConnection(conn);
        }
        return conn;
    }
}
