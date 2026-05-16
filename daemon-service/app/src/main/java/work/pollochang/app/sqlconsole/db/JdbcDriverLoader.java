package work.pollochang.app.sqlconsole.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 負責從 libs/ 目錄動態載入 JDBC 驅動程式
 */
public class JdbcDriverLoader {
    private static final Logger logger = LoggerFactory.getLogger(JdbcDriverLoader.class);
    private final Path libsDir;
    private final ConcurrentHashMap<String, Driver> loadedDrivers = new ConcurrentHashMap<>();

    public JdbcDriverLoader(Path libsDir) {
        this.libsDir = libsDir;
    }

    public void loadDrivers() {
        File folder = libsDir.toFile();
        if (!folder.exists() || !folder.isDirectory()) {
            logger.warn("Libs directory not found: {}", libsDir);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return;

        for (File file : files) {
            try {
                logger.info("Loading driver from jar: {}", file.getName());
                URL url = file.toURI().toURL();
                URLClassLoader ucl = new URLClassLoader(new URL[]{url}, this.getClass().getClassLoader());
                
                // 使用 ServiceLoader 在該 ClassLoader 內尋找 Driver 實作
                java.util.ServiceLoader<Driver> sl = java.util.ServiceLoader.load(Driver.class, ucl);
                for (Driver d : sl) {
                    logger.info("Registering driver: {}", d.getClass().getName());
                    // 必須使用 DriverShim 封裝，因為 DriverManager 只允許由系統 ClassLoader 或當前 ClassLoader 載入的驅動
                    DriverManager.registerDriver(new DriverShim(d));
                }
                logger.info("Successfully processed jar: {}", file.getName());
            } catch (Exception e) {
                logger.error("Failed to load jar: " + file.getName(), e);
            }
        }
    }

    /**
     * 手動註冊驅動 (解決 ClassLoader 隔離問題)
     */
    public static class DriverShim implements Driver {
        private final Driver driver;
        public DriverShim(Driver d) { this.driver = d; }
        public boolean acceptsURL(String u) throws SQLException { return this.driver.acceptsURL(u); }
        public java.sql.Connection connect(String u, java.util.Properties p) throws SQLException { return this.driver.connect(u, p); }
        public int getMajorVersion() { return this.driver.getMajorVersion(); }
        public int getMinorVersion() { return this.driver.getMinorVersion(); }
        public java.sql.DriverPropertyInfo[] getPropertyInfo(String u, java.util.Properties p) throws SQLException { return this.driver.getPropertyInfo(u, p); }
        public boolean jdbcCompliant() { return this.driver.jdbcCompliant(); }
        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException { return this.driver.getParentLogger(); }
    }
}
