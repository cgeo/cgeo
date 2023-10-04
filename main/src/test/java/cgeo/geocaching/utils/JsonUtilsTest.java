package cgeo.geocaching.utils;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class JsonUtilsTest {

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
}
