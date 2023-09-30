package cgeo.geocaching.utils.config;

import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.functions.Func1;

import java.util.List;

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
            if (array.size() > 0) {
                node.set(FIELD_CHILDREN, array);
            }
        }
        return node;
    }

}
