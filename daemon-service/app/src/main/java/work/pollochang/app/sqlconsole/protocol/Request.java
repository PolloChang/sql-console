package work.pollochang.app.sqlconsole.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Request(
    @JsonProperty("version") String version,
    @JsonProperty("requestId") String requestId,
    @JsonProperty("action") String action,
    @JsonProperty("osUser") String osUser,
    @JsonProperty("payload") Object payload
) {
    @Override
    public String toString() {
        return "Request[" +
                "version=" + version + ", " +
                "requestId=" + requestId + ", " +
                "action=" + action + ", " +
                "osUser=" + osUser + ", " +
                "payload=" + work.pollochang.app.sqlconsole.util.MaskingUtils.maskPayload(payload) +
                "]";
    }
}
