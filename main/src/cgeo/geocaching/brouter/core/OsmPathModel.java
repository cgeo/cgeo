/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import java.util.Map;

import cgeo.geocaching.brouter.expressions.BExpressionContextNode;
import cgeo.geocaching.brouter.expressions.BExpressionContextWay;


abstract class OsmPathModel {
    public abstract OsmPrePath createPrePath();

    public abstract OsmPath createPath();

    public abstract void init(BExpressionContextWay expctxWay, BExpressionContextNode expctxNode, Map<String, String> keyValues);
}
