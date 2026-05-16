package work.pollochang.app.sqlconsole.domain;

/**
 * Value Object for Profile Name.
 */
public record ProfileName(String value) {
    public ProfileName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Profile name cannot be empty");
        }
    }
}
