package work.pollochang.app.sqlconsole.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JdbcProfileTest {

    @Test
    void testToStringMasksPassword() {
        JdbcProfile profile = new JdbcProfile("test", "jdbc:postgresql://localhost/db", "user", "secret_pass");
        String str = profile.toString();
        
        assertTrue(str.contains("test"), "Should contain profile name");
        assertTrue(str.contains("user"), "Should contain username");
        assertFalse(str.contains("secret_pass"), "Should NOT contain actual password");
        assertTrue(str.contains("********"), "Should contain masked password");
    }

    @Test
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> new JdbcUrl("invalid-url"));
        assertThrows(IllegalArgumentException.class, () -> new Username(" "));
        assertThrows(IllegalArgumentException.class, () -> new SqlContent(""));
    }
}
