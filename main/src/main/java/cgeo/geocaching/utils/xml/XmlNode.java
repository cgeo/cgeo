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

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Represents the content of an XML Node and its children.
 * <br>
 * Instances of this class are optimized for fast random access to child nodes by their local name.
 * Lists (=many child nodes with same local name) are supported. Note that tags with same local name but different namespace are also stored in list.
 */
public class XmlNode {

    public static final String ATTRIBUTE_PRAEFIX = "@";

    private final String localName;
    private final String namespace;

    private String value;
    private Map<String, Object> childrenMap;
    private final int tagIdx;

    private XmlNode(final String name, final String namespace, final int tagIdx) {
        this.localName = XmlUtils.getLocalName(name);
        this.namespace = StringUtils.isBlank(namespace) ? null : namespace.trim();
        this.tagIdx = tagIdx;
    }

    public String getLocalName() {
        return localName;
    }

    public String getNamespace() {
        return namespace;
    }

    public int getTagIdx() {
        return tagIdx;
    }

    @SuppressWarnings("unchecked")
    private void addChild(final XmlNode child) {

        if (childrenMap == null) {
            childrenMap = new HashMap<>();
        }
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

    public boolean hasChild(final String name) {
        return hasChild(name, null, true);
    }

    public boolean hasChild(final String name, final Set<String> namespaces, final boolean allowNullNamespace) {
        if (childrenMap == null || !childrenMap.containsKey(name)) {
            return false;
        }
        return getChild(name, namespaces, allowNullNamespace) != null;
    }

    public List<XmlNode> getChildrenAsList(final String name) {
        return getChildrenAsList(name, null, true);
    }

    @SuppressWarnings("unchecked")
    public List<XmlNode> getChildrenAsList(final String name, final Set<String> namespaces, final boolean allowNullNamespace) {
        final Object value = childrenMap == null ? null : childrenMap.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof XmlNode) {
            return namespaceMatches((XmlNode) value, namespaces, allowNullNamespace) ? Collections.singletonList((XmlNode) value) : null;
        }
        final List<XmlNode> children = (List<XmlNode>) value;
        if (namespaces == null) {
            return children;
        }

        final boolean allValid = children.stream().allMatch(child -> namespaceMatches(child, namespaces, allowNullNamespace));
        return allValid ? (List<XmlNode>) value : ((List<XmlNode>) value).stream().filter(child -> namespaceMatches(child, namespaces, allowNullNamespace)).toList();
    }

    public XmlNode getChild(final String name) {
        return getChild(name, null, true);
    }

    @SuppressWarnings("unchecked")
    public XmlNode getChild(final String name, final Set<String> namespaces, final boolean allowNullNamespace) {
        final Object value = childrenMap == null ? null : childrenMap.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof XmlNode) {
            return namespaceMatches((XmlNode) value, namespaces, allowNullNamespace) ? (XmlNode) value : null;
        }
        for (XmlNode child : (List<XmlNode>) value) {
            if (namespaceMatches(child, namespaces, allowNullNamespace)) {
                return child;
            }
        }
        return null;
    }

    @Nullable
    public String getChildValue(final String name) {
        final XmlNode child = getChild(name);
        return child == null ? null : child.getValue();
    }

    private static boolean namespaceMatches(@NonNull final XmlNode node, final Set<String> namespaces, final boolean allowNullNamespace) {
        return namespaces == null || (node.namespace == null ? allowNullNamespace : namespaces.contains(node.namespace));
    }

    /** Sets the textual value of this node */
    private void setValue(final String value) {
        if (value == null || StringUtils.isBlank(value) || "nil".equals(value)) {
            this.value = null;
        } else {
            this.value = value;
        }
    }

    public String getValue() {
        return value;
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
        return scanNode(parser, new int[] { 0 });
    }

    private static XmlNode scanNode(final XmlPullParser parser, final int[] tagIdx) throws XmlPullParserException, IOException {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("Not a start tag: " + parser);
        }
        final XmlNode node = new XmlNode(parser.getName(), parser.getNamespace(), tagIdx[0]);
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            final XmlNode attNode = new XmlNode(ATTRIBUTE_PRAEFIX + parser.getAttributeName(i), parser.getAttributeNamespace(i), -1);
            attNode.setValue(parser.getAttributeValue(i));
            node.addChild(attNode);
        }

        while (true) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    return node;
                case XmlPullParser.START_TAG:
                    tagIdx[0]++;
                    node.addChild(scanNode(parser, tagIdx));
                    break;
                case XmlPullParser.TEXT:
                    node.setValue(parser.getText());
                    break;
                default:
                    throw new XmlPullParserException("Unexpected element type: " + parser.getEventType());
            }
        }
    }

}
