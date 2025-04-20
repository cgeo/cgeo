package cgeo.geocaching.utils.xml;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Reprents the content of an XML Node and it's children. Sort of a poor-mans DOM model.
 * <br>
 * Instances of this class are optimized for fast random access to child nodes by their (local) name.
 * Although lists (=many children of same node with same common name) are also supported, order
 * of tags from original document is not preserved.
 */
public class XmlNode {

    public static final String ATTRIBUTE_PRAEFIX = "@";

    private final String name;
    private final String namespace;

    private String value;
    private Map<String, Object> childrenMap;

    public XmlNode(final String name) {
        this(name, null);
    }

    public XmlNode(final String name, final String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    @SuppressWarnings("unchecked")
    public void add(final XmlNode child) {

        if (childrenMap == null) {
            childrenMap = new HashMap<>();
        }
        final Object currentValue = childrenMap.get(child.name);
        if (currentValue instanceof XmlNode) {
            final List<XmlNode> list = new ArrayList<>();
            list.add((XmlNode) currentValue);
            list.add(child);
            childrenMap.put(child.name, list);
        } else if (currentValue instanceof List) {
            ((List<XmlNode>) currentValue).add(child);
        } else {
            childrenMap.put(child.name, child);
        }
    }

    public void remove(final String name) {
        if (childrenMap != null) {
            childrenMap.remove(name);
        }
    }

    public boolean has(final String name) {
        return childrenMap != null && childrenMap.containsKey(name);
    }

    @SuppressWarnings("unchecked")
    public List<XmlNode> getAsList(final String name) {
        final Object value = childrenMap == null ? null : childrenMap.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            return (List<XmlNode>) value;
        }
        return Collections.singletonList((XmlNode) value);
    }

    @SuppressWarnings("unchecked")
    public XmlNode get(final String name) {
        final Object value = childrenMap == null ? null : childrenMap.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            final List<XmlNode> list = (List<XmlNode>) value;
            return list.isEmpty() ? null : list.get(0);
        }
        return (XmlNode) value;
    }

    /** Sets the textual value of this node */
    public void setValue(final String value) {
        if (value == null || StringUtils.isBlank(value)) {
            this.value = null;
        } else {
            this.value = value;
        }
    }

    public String getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public void forEach(final Consumer<XmlNode> action) {
        for (Map.Entry<String, Object> entry : childrenMap.entrySet()) {
            if (entry.getValue() instanceof List) {
                ((List<XmlNode>) entry.getValue()).forEach(action);
            } else {
                action.accept((XmlNode) entry.getValue());
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return (namespace == null ? "" : namespace + ":") + name +
            (value == null ? "" : ":'" + value + "'") +
            (childrenMap == null ? "" : "[" + childrenMap.values() + "]");
    }

    /**
     * Scans an XMLNode from a Pull Parser.
     * Pull Parser has to be placed upon a START_TAG element.
     * When this method returns, pull parser will be placed on the corresponding END_TAG element.
     */
    public static XmlNode scanNode(final XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("Not a start tag: " + parser);
        }
        final XmlNode node = new XmlNode(parser.getName(), parser.getNamespace());
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            final XmlNode attNode = new XmlNode(ATTRIBUTE_PRAEFIX + parser.getAttributeName(i), parser.getAttributeNamespace(i));
            attNode.setValue(parser.getAttributeValue(i));
            node.add(attNode);
        }

        while (true) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    return node;
                case XmlPullParser.START_TAG:
                    node.add(scanNode(parser));
                    break;
                case XmlPullParser.TEXT:
                    node.setValue(parser.getText());
                    break;
                default:
                    throw new XmlPullParserException("Unexpected element tyoe: " + parser.getEventType());
            }
        }
    }

}
