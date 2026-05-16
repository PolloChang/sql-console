package work.pollochang.app.sqlconsole.exception;

/**
 * Exception thrown for database-related errors.
 */
public class DatabaseException extends SqlConsoleException {
    public DatabaseException(String code, String message) {
        super(code, message);
    }

    public DatabaseException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
