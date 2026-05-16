package work.pollochang.app.sqlconsole.exception;

/**
 * Base exception for the sql-console application.
 */
public class SqlConsoleException extends RuntimeException {
    private final String code;

    public SqlConsoleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public SqlConsoleException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
