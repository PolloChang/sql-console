package work.pollochang.app.sqlconsole.exception;

/**
 * Exception thrown for protocol-related errors.
 */
public class ProtocolException extends SqlConsoleException {
    public ProtocolException(String code, String message) {
        super(code, message);
    }

    public ProtocolException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
