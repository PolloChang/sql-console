package work.pollochang.app.sqlconsole.domain;

/**
 * Value object for Request ID.
 */
public record RequestId(String id) {
    public static RequestId of(String id) {
        return new RequestId(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
