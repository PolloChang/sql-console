package work.pollochang.app.sqlconsole.db;

import com.zaxxer.hikari.HikariDataSource;
import work.pollochang.app.sqlconsole.domain.JdbcProfile;
import work.pollochang.app.sqlconsole.exception.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for managing database connections and integrating with the pool manager.
 */
public class ConnectionManager {
    private final ConnectionPoolManager poolManager;

    public ConnectionManager(ConnectionPoolManager poolManager) {
        this.poolManager = poolManager;
    }

    /**
     * Obtains a connection for the given profile.
     */
    public Connection getConnection(JdbcProfile profile) {
        try {
            HikariDataSource ds = poolManager.getOrCreatePool(profile);
            return ds.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException("DB-001", "Failed to get connection for profile: " + profile.profileName().value(), e);
        }
    }

    /**
     * Validates a profile by attempting to connect.
     */
    public void validateProfile(JdbcProfile profile) {
        try (Connection conn = getConnection(profile)) {
            conn.getMetaData();
        } catch (SQLException e) {
            throw new DatabaseException("DB-001", "Validation failed for profile: " + profile.profileName().value(), e);
        }
    }
}
