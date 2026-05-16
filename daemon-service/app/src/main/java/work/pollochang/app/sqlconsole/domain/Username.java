package work.pollochang.app.sqlconsole.domain;

/**
 * Value Object for Database Username.
 */
public record Username(String value) {
    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
    }
}
