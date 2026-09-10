package cgeo.geocaching.utils.xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Represents the content of an XML Node and its children. Sort of a poor-mans DOM model.
 * <br>
 * Instances of this class are optimized for fast random access to child nodes by their local name.
 * Lists (=many child nodes with same local name) are supported. Note that tags with same local name but different namespace are also stored in list.
 * Child insertion order is available separately from the local-name index.
 */
public class XmlNode {

    public static final String ATTRIBUTE_PRAEFIX = "@";

    private final String localName;
    private final String namespace;

    private String value;
    private Map<String, Object> childrenMap;
    private List<XmlNode> orderedChildren;

    public XmlNode(final String name, final String namespace) {
        this.localName = XmlUtils.getLocalName(name);
        this.namespace = namespace;
    }

    public String getLocalName() {
        return localName;
    }

    public String getNamespace() {
        return namespace;
    }

    @SuppressWarnings("unchecked")
    public void addChild(final XmlNode child) {

        if (childrenMap == null) {
            childrenMap = new HashMap<>();
            orderedChildren = new ArrayList<>();
        }
        orderedChildren.add(child);
        final Object currentValue = childrenMap.get(child.localName);
        if (currentValue instanceof XmlNode) {
            final List<XmlNode> list = new ArrayList<>();
            list.add((XmlNode) currentValue);
            list.add(child);
            childrenMap.put(child.localName, list);
        } else if (currentValue instanceof List) {
            ((List<XmlNode>) currentValue).add(child);
        } else {
            childrenMap.put(child.localName, child);
        }
    }

    public void removeChild(final String name) {
        if (childrenMap != null) {
            childrenMap.remove(name);
            orderedChildren.removeIf(child -> child.localName.equals(name));
        }
    }

    /** All children (including attribute nodes) in insertion order. */
    public List<XmlNode> getChildrenInOrder() {
        return orderedChildren == null ? Collections.emptyList() : Collections.unmodifiableList(orderedChildren);
    }

    public boolean hasChild(final String name) {
        return childrenMap != null && childrenMap.containsKey(name);
    }

    public int countChildren(final String name) {
        final Object value = childrenMap == null ? null : childrenMap.get(name);
        if (value == null) {
            return 0;
        }
        if (value instanceof List) {
            return ((List<?>) value).size();
        }
        return 1;
    }

    @SuppressWarnings("unchecked")
    public List<XmlNode> getChildrenAsList(final String name) {
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
    public XmlNode getChild(final String name) {
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

    @Nullable
    public String getChildValue(final String name) {
        final XmlNode child = getChild(name);
        return child == null ? null : child.getValue();
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
        if (childrenMap == null) {
            return;
        }
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
        return (namespace == null ? "" : namespace + ":") + localName +
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
            node.addChild(attNode);
        }

        while (true) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    return node;
                case XmlPullParser.START_TAG:
                    node.addChild(scanNode(parser));
                    break;
                case XmlPullParser.TEXT:
                    node.setValue(parser.getText());
                    break;
                default:
                    throw new XmlPullParserException("Unexpected element tyoe: " + parser.getEventType());
            }
        }
    }

    @Nullable
    public static XmlNode getChild(@Nullable final XmlNode node, @Nullable final String name, @Nullable final Set<String> preferredNamespace) {
        if (node == null) {
            return null;
        }
        if (node.countChildren(name) <= 1 || preferredNamespace == null) {
            return node.getChild(name);
        }
        final List<XmlNode> children = node.getChildrenAsList(name);
        for (XmlNode child : children) {
            if (preferredNamespace.contains(child.getNamespace())) {
                return child;
            }
        }
        return children.get(0);
    }

}
