package cgeo.geocaching.files;

import cgeo.geocaching.utils.xml.XmlNode;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

abstract class GPXMultiParserBase {
    protected final ArrayList<XmlNode> nodes = new ArrayList<>();

    abstract String getNodeName();

    boolean handlesNode(final String node) {
        return (node != null && StringUtils.equals(node, getNodeName()));
    }

    void addNode(@NonNull final XmlNode node) {
        nodes.add(node);
    }

    abstract void onParsingDone(@NonNull Collection<Object> result);
}
