package work.pollochang.app.sqlconsole.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Message(
    @JsonProperty("version") String version,
    @JsonProperty("requestId") String requestId,
    @JsonProperty("type") String type,
    @JsonProperty("payload") Object payload
) {
    public static Message header(String requestId, Object payload) {
        return new Message("1.0", requestId, "header", payload);
    }

    public static Message row(String requestId, Object payload) {
        return new Message("1.0", requestId, "row", payload);
    }

    public static Message footer(String requestId, Object payload) {
        return new Message("1.0", requestId, "footer", payload);
    }

    public static Message success(String requestId, Object payload) {
        return new Message("1.0", requestId, "success", payload);
    }

    public static Message error(String requestId, Object payload) {
        return new Message("1.0", requestId, "error", payload);
    }
}
