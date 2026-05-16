package work.pollochang.app.sqlconsole.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for masking sensitive information in logs and messages.
 */
public class MaskingUtils {

    /**
     * Masks sensitive information in SQL strings (e.g., PASSWORD, IDENTIFIED BY).
     */
    public static String maskSql(String sql) {
        if (sql == null) return null;
        return sql.replaceAll("(?i)(PASSWORD\\s+['\"]?)([^'\"\\s;]+)(['\"]?)", "$1***$3")
                  .replaceAll("(?i)(IDENTIFIED\\s+BY\\s+['\"]?)([^'\"\\s;]+)(['\"]?)", "$1***$3");
    }

    /**
     * Masks sensitive fields in a payload object (usually a Map).
     */
    @SuppressWarnings("unchecked")
    public static Object maskPayload(Object payload) {
        if (payload instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) payload;
            if (map.containsKey("password") || map.containsKey("secret") || map.containsKey("token")) {
                Map<String, Object> masked = new HashMap<>(map);
                if (masked.containsKey("password")) masked.put("password", "***");
                if (masked.containsKey("secret")) masked.put("secret", "***");
                if (masked.containsKey("token")) masked.put("token", "***");
                return masked;
            }
            return map;
        }
        if (payload instanceof String) {
            return maskSql((String) payload);
        }
        return payload;
    }
}
