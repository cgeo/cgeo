package cgeo.geocaching.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();
    public static final ObjectReader reader = mapper.reader();
    public static final ObjectWriter writer = mapper.writer();

    public static final JsonNodeFactory factory = new JsonNodeFactory(true);

    private JsonUtils() {
        // Do not instantiate
    }

    public static JsonNode toNode(final String jsonString) {
        if (jsonString == null) {
            return null;
        }
        try {
            return mapper.readTree(jsonString);
        } catch (JsonProcessingException jpe) {
            Log.w("Could not process json '" + jsonString + "'", jpe);
            return null;
        }
    }

    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return mapper.createArrayNode();
    }

    public static String toString(final JsonNode node) {
        return node.toString();
    }

}
