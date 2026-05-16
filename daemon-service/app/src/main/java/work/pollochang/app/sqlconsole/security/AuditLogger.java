package work.pollochang.app.sqlconsole.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.pollochang.app.sqlconsole.domain.JdbcProfile;

/**
 * Handles security auditing for the sql-console daemon.
 * All security-related events should be logged through this class.
 */
public class AuditLogger {
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    public void logServerStart(String socketPath) {
        auditLogger.info("SERVER_START: Socket Path={}", socketPath);
    }

    public void logServerStop() {
        auditLogger.info("SERVER_STOP");
    }

    public void logConnectionAttempt(String osUser, String profileName, String dbUser, String remoteAddress) {
        auditLogger.info("CONN_ATTEMPT: OS_User={}, Profile={}, DB_User={}, Remote={}", osUser, profileName, dbUser, remoteAddress);
    }

    public void logConnectionSuccess(String osUser, String profileName, String dbUser) {
        auditLogger.info("CONN_SUCCESS: OS_User={}, Profile={}, DB_User={}", osUser, profileName, dbUser);
    }

    public void logConnectionFailure(String osUser, String profileName, String dbUser, String reason) {
        auditLogger.warn("CONN_FAILURE: OS_User={}, Profile={}, DB_User={}, Reason={}", osUser, profileName, dbUser, reason);
    }

    public void logQueryStart(String osUser, String profileName, String dbUser, String sql) {
        String safeSql = maskSensitiveInfo(sql);
        // RFP says "which SQL was executed". Limit to 1000 chars for safety in logs.
        if (safeSql != null && safeSql.length() > 1000) {
            safeSql = safeSql.substring(0, 997) + "...";
        }
        auditLogger.info("QUERY_START: OS_User={}, Profile={}, DB_User={}, SQL=[{}]", osUser, profileName, dbUser, safeSql);
    }

    public void logSecurityViolation(String osUser, String action, String reason) {
        auditLogger.error("SECURITY_VIOLATION: OS_User={}, Action={}, Reason={}", osUser, action, reason);
    }

    private String maskSensitiveInfo(String sql) {
        return work.pollochang.app.sqlconsole.util.MaskingUtils.maskSql(sql);
    }
}
