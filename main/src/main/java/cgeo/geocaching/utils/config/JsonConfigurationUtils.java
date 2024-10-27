package cgeo.geocaching.utils.config;

import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.functions.Func1;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public final class JsonConfigurationUtils {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CONFIG = "config";
    private static final String FIELD_CHILDREN = "children";

    private JsonConfigurationUtils() {
        //no instance
    }

    public static JsonNode toJsonConfig(final IJsonConfigurable<?> expression) {
        return expression == null ? null : createJsonNode(expression);
    }

    public static String toJsonConfigString(final IJsonConfigurable<?> expression) {
        return expression == null ? null : JsonUtils.nodeToString(toJsonConfig(expression));
    }

    public static <T extends IJsonConfigurable<T>> T fromJsonConfig(final JsonNode node, final Func1<String, T> expressionCreator) {
        if (node == null || !node.has(FIELD_TYPE)) {
            return null;
        }
        final T expression = expressionCreator.call(node.get(FIELD_TYPE).textValue());
        if (expression == null) {
            return null;
        }
        final JsonNode configNode = node.get(FIELD_CONFIG);
        if (configNode instanceof ObjectNode) {
            expression.setJsonConfig((ObjectNode) configNode);
        }
        if (node.has(FIELD_CHILDREN)) {
            final ArrayNode children = (ArrayNode) node.get(FIELD_CHILDREN);
            for (JsonNode child : children) {
                final T childExpression = fromJsonConfig(child, expressionCreator);
                if (childExpression != null) {
                    expression.addChild(childExpression);
                }
            }
        }
        return expression;
    }

    public static <T extends IJsonConfigurable<T>> T fromJsonConfig(final String json, final Func1<String, T> expressionCreator) {
        if (json == null) {
            return null;
        }

        return fromJsonConfig(JsonUtils.stringToNode(json), expressionCreator);
    }

    /** Helper method to compare two IJsonComfigration objects using their JSON representation */
    public static boolean equals(final IJsonConfigurable<?> jsonConfig1, final IJsonConfigurable<?> jsonConfig2) {
        if (jsonConfig1 == jsonConfig2) {
            return true;
        }
        if (jsonConfig1 == null || jsonConfig2 == null) {
            return false;
        }
        final JsonNode node1 = toJsonConfig(jsonConfig1);
        final JsonNode node2 = toJsonConfig(jsonConfig2);

        return Objects.equals(node1, node2);
    }

    /** Helper method to implement HashCode for an object implementing IJsonConfiguration */
    public static int hashCode(final IJsonConfigurable<?> jsonConfig) {
        if (jsonConfig == null) {
            return 7;
        }
        final JsonNode node = jsonConfig.getJsonConfig();
        return node == null ? 13 : node.hashCode();
    }


        private static <T extends IJsonConfigurable<T>> JsonNode createJsonNode(final IJsonConfigurable<T> expression) {
        if (expression == null) {
            return null;
        }
        final ObjectMapper mapper = JsonUtils.mapper;

        final ObjectNode node = mapper.createObjectNode();
        node.set(FIELD_TYPE, new TextNode(expression.getId()));
        final JsonNode config = expression.getJsonConfig();
        if (config != null) {
            node.set(FIELD_CONFIG, config);
        }
        final List<T> children = expression.getChildren();
        if (children != null && !children.isEmpty()) {
            final ArrayNode array = mapper.createArrayNode();
            for (IJsonConfigurable<?> child : children) {
                final JsonNode childNode = createJsonNode(child);
                if (childNode != null) {
                    array.add(childNode);
                }
            }
            if (!array.isEmpty()) {
                node.set(FIELD_CHILDREN, array);
            }
        }
        return node;
    }

}
