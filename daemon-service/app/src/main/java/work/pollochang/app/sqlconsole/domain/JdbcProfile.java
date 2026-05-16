package work.pollochang.app.sqlconsole.domain;

/**
 * Represents a database connection profile.
 * Uses Value Objects to ensure type safety and validation.
 */
public record JdbcProfile(
    ProfileName profileName,
    JdbcUrl url,
    Username username,
    Password password
) {
    // Convenience constructor for strings (useful for protocol parsing)
    public JdbcProfile(String profileName, String url, String username, String password) {
        this(new ProfileName(profileName), new JdbcUrl(url), new Username(username), new Password(password));
    }
}
