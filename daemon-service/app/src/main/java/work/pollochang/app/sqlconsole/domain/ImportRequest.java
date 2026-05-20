package work.pollochang.app.sqlconsole.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImportRequest(
    @JsonProperty("table") String table,
    @JsonProperty("columns") List<String> columns,
    @JsonProperty("rows") List<List<Object>> rows
) {}
