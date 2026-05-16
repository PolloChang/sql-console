package work.pollochang.app.sqlconsole.domain;

/**
 * Value Object for Database Password.
 * Encapsulates password and prevents accidental exposure via toString().
 */
public record Password(String value) {
    public Password {
        if (value == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
    }

    @Override
    public String toString() {
        return "********";
    }
}
