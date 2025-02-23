package cgeo.geocaching.files;

import cgeo.geocaching.utils.xml.XmlNode;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

abstract class GPXMultiParserBase {
    protected final ArrayList<XmlNode> nodes = new ArrayList<>();

    abstract String getNodeName();

    void addNode(@NonNull final XmlNode node) {
        nodes.add(node);
    }

    abstract void onParsingDone(@NonNull Collection<Object> result);
}
