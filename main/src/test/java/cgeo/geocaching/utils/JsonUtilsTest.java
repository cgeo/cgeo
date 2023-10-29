package cgeo.geocaching.utils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class JsonUtilsTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class JsonDateTestClass {
        @JsonProperty("logDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN)
        Date logDate;
    }

    private static final String TEST_JSON = "{\n" +
            "   \"color\": \"green\",\n" +
            "   \"age\": 12,\n" +
            "   \"darkmode\": true,\n" +
            "   \"temperature\": 20.4,\n" +
            "   \"animals\": [\"dog\", \"cat\", \"bird\" ],\n" +
            "   \"empty\": null,\n" +
            "   \"mixed\": [\"red\", 5, null, 3.1, true, \"blue\"]\n" +
            "}";

    @Test
    public void parseAndWrite() {
        final String json = TEST_JSON;

        final JsonNode node = JsonUtils.stringToNode(json, false);
        final String json2 = JsonUtils.nodeToString(node);

        assertThat(json2).isEqualToIgnoringWhitespace(json);
    }

    @Test(expected = Exception.class)
    public void illegalJson() {
        JsonUtils.stringToNode("{:", false);
    }

    @Test
    public void readParsedJson() {
        final JsonNode node = JsonUtils.stringToNode(TEST_JSON, false);

        //normal stuff
        assertThat(JsonUtils.toText(JsonUtils.get(node, "color"), null)).isEqualTo("green");
        assertThat(JsonUtils.toFloat(JsonUtils.get(node, "age"), null)).isEqualTo(12);
        assertThat(JsonUtils.toBoolean(JsonUtils.get(node, "darkmode"), null)).isEqualTo(true);
        assertThat(JsonUtils.toFloat(JsonUtils.get(node, "temperature"), null)).isEqualTo(20.4f);

        //array
        assertThat(JsonUtils.toList(JsonUtils.get(node, "animals"), n -> JsonUtils.toText(n, null))).containsExactly("dog", "cat", "bird");

        //empty and null
        assertThat(JsonUtils.get(node, "empty")).isNull();
        assertThat(JsonUtils.get(node, "nonexisting")).isNull();
        assertThat(JsonUtils.toText(JsonUtils.get(node, "empty"), "defaultValue")).isEqualTo("defaultValue");

        //mixed array
        assertThat(JsonUtils.toList(JsonUtils.get(node, "mixed"), n -> JsonUtils.toText(n, null)))
                .containsExactly("red", "5", null, "3.1", "true", "blue");

    }

    @Test
    public void writeJson() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.set(node, "empty", null);
        JsonUtils.set(node, "text", JsonUtils.fromText("green"));
        JsonUtils.set(node, "float", JsonUtils.fromFloat(1f));
        JsonUtils.set(node, "boolean", JsonUtils.fromBoolean(true));
        JsonUtils.set(node, "booleanNull", JsonUtils.fromBoolean(null));
        JsonUtils.set(node, "array", JsonUtils.fromCollection(Arrays.asList("red", null, "blue"), JsonUtils::fromText));

        final String json = JsonUtils.nodeToString(node);
        assertThat(json).isEqualToIgnoringWhitespace("{\"text\":\"green\",\"float\":1.0,\"boolean\":true,\"array\":[\"red\",null,\"blue\"]}");
    }

    @Test
    public void dateWithTimezones() throws JsonProcessingException {
        final Date now = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat(JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN, Locale.ENGLISH);
        final String sdfDateString = sdf.format(now);

        final JsonDateTestClass jsonDate = new JsonDateTestClass();
        jsonDate.logDate = now;
        final String json = JsonUtils.mapper.writeValueAsString(jsonDate);
        final String jsonDateString = JsonUtils.stringToNode(json).get("logDate").textValue();

        assertThat(jsonDateString).isEqualTo(sdfDateString);

    }
}
