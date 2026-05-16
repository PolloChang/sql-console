package work.pollochang.app.sqlconsole.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import work.pollochang.app.sqlconsole.domain.JdbcProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages HikariCP connection pools.
 */
public class ConnectionPoolManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolManager.class);
    
    // Key: Profile Name
    private final ConcurrentHashMap<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    public HikariDataSource getOrCreatePool(JdbcProfile profile) {
        return dataSources.computeIfAbsent(profile.profileName().value(), name -> {
            logger.info("Creating new HikariCP pool for profile: {}", name);
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(profile.url().value());
            config.setUsername(profile.username().value());
            config.setPassword(profile.password().value());
            
            // Per RFP: Max connections 5, Idle timeout 10 minutes
            config.setMaximumPoolSize(5);
            config.setIdleTimeout(600000); // 10 minutes
            config.setPoolName("HikariPool-" + name);
            
            // Performance optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            return new HikariDataSource(config);
        });
    }

    public void shutdownPool(String poolName) {
        HikariDataSource ds = dataSources.remove(poolName);
        if (ds != null) {
            logger.info("Shutting down pool: {}", poolName);
            ds.close();
        }
    }

    public void shutdownAll() {
        dataSources.forEach((name, ds) -> {
            logger.info("Shutting down pool during system exit: {}", name);
            ds.close();
        });
        dataSources.clear();
    }
}
