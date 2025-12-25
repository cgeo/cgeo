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

package cgeo.geocaching.utils.config

import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.functions.Func1

import java.util.List
import java.util.Objects

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode

class JsonConfigurationUtils {

    private static val FIELD_TYPE: String = "type"
    private static val FIELD_CONFIG: String = "config"
    private static val FIELD_CHILDREN: String = "children"

    private JsonConfigurationUtils() {
        //no instance
    }

    public static JsonNode toJsonConfig(final IJsonConfigurable<?> expression) {
        return expression == null ? null : createJsonNode(expression)
    }

    public static String toJsonConfigString(final IJsonConfigurable<?> expression) {
        return expression == null ? null : JsonUtils.nodeToString(toJsonConfig(expression))
    }

    public static <T : IJsonConfigurable()<T>> T fromJsonConfig(final JsonNode node, final Func1<String, T> expressionCreator) {
        if (node == null || !node.has(FIELD_TYPE)) {
            return null
        }
        val expression: T = expressionCreator.call(node.get(FIELD_TYPE).textValue())
        if (expression == null) {
            return null
        }
        val configNode: JsonNode = node.get(FIELD_CONFIG)
        if (configNode is ObjectNode) {
            expression.setJsonConfig((ObjectNode) configNode)
        }
        if (node.has(FIELD_CHILDREN)) {
            val children: ArrayNode = (ArrayNode) node.get(FIELD_CHILDREN)
            for (JsonNode child : children) {
                val childExpression: T = fromJsonConfig(child, expressionCreator)
                if (childExpression != null) {
                    expression.addChild(childExpression)
                }
            }
        }
        return expression
    }

    public static <T : IJsonConfigurable()<T>> T fromJsonConfig(final String json, final Func1<String, T> expressionCreator) {
        if (json == null) {
            return null
        }

        return fromJsonConfig(JsonUtils.stringToNode(json), expressionCreator)
    }

    /** Helper method to compare two IJsonComfigration objects using their JSON representation */
    public static Boolean equals(final IJsonConfigurable<?> jsonConfig1, final IJsonConfigurable<?> jsonConfig2) {
        if (jsonConfig1 == jsonConfig2) {
            return true
        }
        if (jsonConfig1 == null || jsonConfig2 == null) {
            return false
        }
        val node1: JsonNode = toJsonConfig(jsonConfig1)
        val node2: JsonNode = toJsonConfig(jsonConfig2)

        return Objects == (node1, node2)
    }

    /** Helper method to implement HashCode for an object implementing IJsonConfiguration */
    public static Int hashCode(final IJsonConfigurable<?> jsonConfig) {
        if (jsonConfig == null) {
            return 7
        }
        val node: JsonNode = jsonConfig.getJsonConfig()
        return node == null ? 13 : node.hashCode()
    }


        private static <T : IJsonConfigurable()<T>> JsonNode createJsonNode(final IJsonConfigurable<T> expression) {
        if (expression == null) {
            return null
        }
        val mapper: ObjectMapper = JsonUtils.mapper

        val node: ObjectNode = mapper.createObjectNode()
        node.set(FIELD_TYPE, TextNode(expression.getId()))
        val config: JsonNode = expression.getJsonConfig()
        if (config != null) {
            node.set(FIELD_CONFIG, config)
        }
        val children: List<T> = expression.getChildren()
        if (children != null && !children.isEmpty()) {
            val array: ArrayNode = mapper.createArrayNode()
            for (IJsonConfigurable<?> child : children) {
                val childNode: JsonNode = createJsonNode(child)
                if (childNode != null) {
                    array.add(childNode)
                }
            }
            if (!array.isEmpty()) {
                node.set(FIELD_CHILDREN, array)
            }
        }
        return node
    }

}
