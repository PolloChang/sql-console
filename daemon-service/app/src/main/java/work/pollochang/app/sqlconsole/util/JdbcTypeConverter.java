package work.pollochang.app.sqlconsole.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to convert vendor-specific JDBC types to standard Java types that are JSON-serializable.
 */
public class JdbcTypeConverter {
    private static final Logger logger = LoggerFactory.getLogger(JdbcTypeConverter.class);
    private static final Map<String, Method> methodCache = new HashMap<>();

    /**
     * Converts a JDBC object to a standard Java object.
     */
    public static Object convert(Object value) {
        if (value == null) {
            return null;
        }

        String className = value.getClass().getName();

        // Handle Oracle types via reflection to avoid hard dependency
        if (className.startsWith("oracle.sql.")) {
            return convertOracleType(value, className);
        }

        // Potential future: Handle other vendor types (SQL Server, IBM DB2, etc.)

        return value;
    }

    private static Object convertOracleType(Object value, String className) {
        try {
            String methodName;
            if (className.endsWith("TIMESTAMP") || className.endsWith("TIMESTAMPTZ") || className.endsWith("TIMESTAMPLTZ")) {
                methodName = "timestampValue";
            } else if (className.endsWith("DATE")) {
                methodName = "dateValue";
            } else if (className.endsWith("CLOB")) {
                methodName = "stringValue";
            } else if (className.endsWith("BLOB")) {
                // Blobs are hard to serialize as JSON, maybe convert to hex or ignore
                return "[BLOB]";
            } else {
                return value.toString();
            }

            Method method = getMethod(value.getClass(), methodName);
            if (method != null) {
                return method.invoke(value);
            }
        } catch (Exception e) {
            logger.warn("Failed to convert Oracle type {}: {}", className, e.getMessage());
        }
        return value.toString();
    }

    private static Method getMethod(Class<?> clazz, String methodName) {
        String key = clazz.getName() + "." + methodName;
        if (methodCache.containsKey(key)) {
            return methodCache.get(key);
        }
        try {
            Method method = clazz.getMethod(methodName);
            methodCache.put(key, method);
            return method;
        } catch (NoSuchMethodException e) {
            methodCache.put(key, null);
            return null;
        }
    }
}
