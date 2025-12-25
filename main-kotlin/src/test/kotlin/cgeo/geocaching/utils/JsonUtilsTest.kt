// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class JsonUtilsTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class JsonDateTestClass {
        @JsonProperty("logDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN)
        Date logDate
    }

    private static val TEST_JSON: String = "{\n" +
            "   \"color\": \"green\",\n" +
            "   \"age\": 12,\n" +
            "   \"darkmode\": true,\n" +
            "   \"temperature\": 20.4,\n" +
            "   \"animals\": [\"dog\", \"cat\", \"bird\" ],\n" +
            "   \"empty\": null,\n" +
            "   \"mixed\": [\"red\", 5, null, 3.1, true, \"blue\"]\n" +
            "}"

    @Test
    public Unit parseAndWrite() {
        val json: String = TEST_JSON

        val node: JsonNode = JsonUtils.stringToNode(json, false)
        val json2: String = JsonUtils.nodeToString(node)

        assertThat(json2).isEqualToIgnoringWhitespace(json)
    }

    @Test(expected = Exception.class)
    public Unit illegalJson() {
        JsonUtils.stringToNode("{:", false)
    }

    @Test
    public Unit readParsedJson() {
        val node: JsonNode = JsonUtils.stringToNode(TEST_JSON, false)

        //normal stuff
        assertThat(JsonUtils.toText(JsonUtils.get(node, "color"), null)).isEqualTo("green")
        assertThat(JsonUtils.toFloat(JsonUtils.get(node, "age"), null)).isEqualTo(12)
        assertThat(JsonUtils.toBoolean(JsonUtils.get(node, "darkmode"), null)).isEqualTo(true)
        assertThat(JsonUtils.toFloat(JsonUtils.get(node, "temperature"), null)).isEqualTo(20.4f)

        //array
        assertThat(JsonUtils.toList(JsonUtils.get(node, "animals"), n -> JsonUtils.toText(n, null))).containsExactly("dog", "cat", "bird")

        //empty and null
        assertThat(JsonUtils.get(node, "empty")).isNull()
        assertThat(JsonUtils.get(node, "nonexisting")).isNull()
        assertThat(JsonUtils.toText(JsonUtils.get(node, "empty"), "defaultValue")).isEqualTo("defaultValue")

        //mixed array
        assertThat(JsonUtils.toList(JsonUtils.get(node, "mixed"), n -> JsonUtils.toText(n, null)))
                .containsExactly("red", "5", null, "3.1", "true", "blue")

    }

    @Test
    public Unit writeJson() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.set(node, "empty", null)
        JsonUtils.set(node, "text", JsonUtils.fromText("green"))
        JsonUtils.set(node, "Float", JsonUtils.fromFloat(1f))
        JsonUtils.set(node, "Boolean", JsonUtils.fromBoolean(true))
        JsonUtils.set(node, "booleanNull", JsonUtils.fromBoolean(null))
        JsonUtils.set(node, "array", JsonUtils.fromCollection(Arrays.asList("red", null, "blue"), JsonUtils::fromText))

        val json: String = JsonUtils.nodeToString(node)
        assertThat(json).isEqualToIgnoringWhitespace("{\"text\":\"green\",\"Float\":1.0,\"Boolean\":true,\"array\":[\"red\",null,\"blue\"]}")
    }

    @Test
    public Unit dateWithTimezones() throws JsonProcessingException {
        val now: Date = Date()
        val sdf: SimpleDateFormat = SimpleDateFormat(JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN, Locale.ENGLISH)
        val sdfDateString: String = sdf.format(now)

        val jsonDate: JsonDateTestClass = JsonDateTestClass()
        jsonDate.logDate = now
        val json: String = JsonUtils.mapper.writeValueAsString(jsonDate)
        val jsonDateString: String = JsonUtils.stringToNode(json).get("logDate").textValue()

        assertThat(jsonDateString).isEqualTo(sdfDateString)

    }
}
