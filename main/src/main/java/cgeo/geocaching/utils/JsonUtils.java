package cgeo.geocaching.utils;

import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/** Helper methods for JSON processing */
public class JsonUtils {

    public static final ObjectMapper mapper = new ObjectMapper();
    public static final ObjectReader reader = mapper.reader();
    public static final ObjectWriter writer = mapper.writer();

    public static final JsonNodeFactory factory = new JsonNodeFactory(true);

    public static final String JSON_LOCAL_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(JSON_LOCAL_TIMESTAMP_PATTERN, Locale.US);

    static {
        //default timezone of Jackson is UTC, but in c:geo we use default (local) timezone. See e.g. #14746
        mapper.setTimeZone(TimeZone.getDefault());
    }


    private JsonUtils() {
        // Do not instantiate
    }

    /** converts a json string to a JsonNode. Fails quietly. */
    public static JsonNode stringToNode(final String jsonString) {
        return stringToNode(jsonString, true);
    }

    public static <T> T readValueFailSilently(final String json, @NonNull final Class<T> clazz, final T defaultValue) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException ie) {
            Log.w("JsonUtils: failed to read value as class " + clazz.getName() + ":'" + json + "'");
            return defaultValue;
        }
    }

    public static JsonNode stringToNode(final String jsonString, final boolean failQuietly) {
        if (jsonString == null) {
            return null;
        }
        try {
            return mapper.readTree(jsonString);
        } catch (IOException e) {
            final String msg = "JSON: Could not convert string to json: '" + jsonString + "'";
            Log.w(msg, e);
            if (failQuietly) {
                return null;
            }
            throw new IllegalArgumentException(msg, e);
        }
    }

    public static String nodeToString(final JsonNode node) {
        return nodeToString(node, true);
    }

    public static String nodeToString(final JsonNode node, final boolean failQuietly) {

        try {
            return node == null ? null : mapper.writeValueAsString(node);
        } catch (IOException e) {
            final String msg = "JSON: Could not parse json to string: " + node;
            Log.w(msg, e);
            if (failQuietly) {
                return null;
            }
            throw new IllegalArgumentException(msg, e);
        }
    }

    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return mapper.createArrayNode();
    }

    /** Sets a field of an ObjectNode, but only if field value is not null */
    public static void set(final ObjectNode node, final String fieldName, final JsonNode value) {
        if (value != null && !(value instanceof NullNode)) {
            node.set(fieldName, value);
        }
    }

    /** Returns a fields value. If field does not exist or is NullNode then null is returned */
    public static JsonNode get(final JsonNode node, final String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        final JsonNode result = node.get(fieldName);
        return result instanceof NullNode ? null : result;
    }

    public static boolean has(final JsonNode node, final String fieldName) {
        return node != null && node.has(fieldName);
    }

    /** constructs a boolean node from a boolean value. Returns null on null value */
    public static JsonNode fromBoolean(final Boolean value) {
        return value == null ? null : BooleanNode.valueOf(value);
    }

    public static void setBoolean(final ObjectNode node, final String fieldName, final Boolean value) {
        set(node, fieldName, fromBoolean(value));
    }

    /** constructs a boolean value from a node. If node is null or not boolean, defaultValue is returned. */
    public static Boolean toBoolean(final JsonNode node, final Boolean defaultValue) {
        return node == null || !node.isBoolean() ? defaultValue : node.booleanValue();
    }

    public static Boolean getBoolean(final JsonNode node, final String fieldName, final Boolean defaultValue) {
        return toBoolean(get(node, fieldName), defaultValue);
    }

    public static JsonNode fromInt(final Integer value) {
        return value == null ? null : new IntNode(value);
    }

    public static void setInt(final ObjectNode node, final String fieldName, final Integer value) {
        set(node, fieldName, fromInt(value));
    }

    public static Integer toInt(final JsonNode node, final Integer defaultValue) {
        return node == null || !node.isNumber() ? defaultValue : node.intValue();
    }

    public static Integer getInt(final JsonNode node, final String fieldName, final Integer defaultValue) {
        return toInt(get(node, fieldName), defaultValue);
    }


    public static JsonNode fromText(final String value) {
        return value == null ? null : new TextNode(value);
    }

    public static void setText(final ObjectNode node, final String fieldName, final String value) {
        set(node, fieldName, fromText(value));
    }


    public static String toText(final JsonNode node, final String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isInt()) {
            return "" + node.asInt();
        }
        if (node.isNumber()) {
            return "" + node.asDouble();
        }
        if (node.isBoolean()) {
            return "" + node.asBoolean();
        }
        return defaultValue;
    }

    public static String getText(final JsonNode node, final String fieldName, final String defaultValue) {
        return toText(get(node, fieldName), defaultValue);
    }

    public static JsonNode fromFloat(final Number value) {
        return value == null ? null : new FloatNode(value.floatValue());
    }
    public static void setFloat(final ObjectNode node, final String fieldName, final Number value) {
        set(node, fieldName, fromFloat(value));
    }

    public static Float toFloat(final JsonNode node, final Float defaultValue) {
        if (node == null || !node.isNumber()) {
            return defaultValue;
        }
        return node.floatValue();
    }

    public static Float getFloat(final JsonNode node, final String fieldName, final Float defaultValue) {
        return toFloat(get(node, fieldName), defaultValue);
    }

    public static JsonNode fromDate(final Date value) {
        return value == null ? null : new TextNode(DATE_FORMAT.format(value));
    }
    public static void setDate(final ObjectNode node, final String fieldName, final Date value) {
        set(node, fieldName, fromDate(value));
    }

    public static Date toDate(final JsonNode node, final Date defaultValue) {
        if (node == null || !node.isTextual()) {
            return defaultValue;
        }
        final String text = node.asText();
        try {
            return DATE_FORMAT.parse(text);
        } catch (ParseException pe) {
            return defaultValue;
        }
    }

    public static Date getDate(final JsonNode node, final String fieldName, final Date defaultValue) {
        return toDate(get(node, fieldName), defaultValue);
    }


    public static <T> ArrayNode fromCollection(final Collection<T> coll, final Func1<T, JsonNode> mapper) {
        if (coll == null) {
            return null;
        }
        final ArrayNode array = createArrayNode();
        for (T v : coll) {
            final JsonNode valueNode = v == null ? null : mapper.call(v);
            array.add(valueNode == null ? NullNode.getInstance() : valueNode);
        }
        return array.isEmpty() ? null : array;
    }

    public static <T> void setCollection(final ObjectNode node, final String fieldName, final Collection<T> coll, final Func1<T, JsonNode> mapper) {
        set(node, fieldName, fromCollection(coll, mapper));
    }

    public static void setTextCollection(final ObjectNode node, final String fieldName, final Collection<String> textColl) {
        setCollection(node, fieldName, textColl, JsonUtils::fromText);
    }


    @NonNull
    public static <T> List<T> toList(final JsonNode node, final Func1<JsonNode, T> mapper) {
        if (node == null || !(node.isArray())) {
            return Collections.emptyList();
        }
        final List<T> result = new ArrayList<>();
        for (JsonNode child : node) {
            final T value = child == null ? null : mapper.call(child);
            result.add(value instanceof NullNode ? null : value);
        }
        return result;
    }

    @NonNull
    public static <T> List<T> getList(final JsonNode node, final String fieldName, final Func1<JsonNode, T> mapper) {
        return toList(get(node, fieldName), mapper);
    }

    public static List<String> getTextList(final JsonNode node, final String fieldName) {
        return getList(node, fieldName, n  -> toText(n, null));
    }



}
