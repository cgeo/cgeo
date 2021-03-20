/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import java.util.Map;

import cgeo.geocaching.brouter.expressions.BExpressionContext;
import cgeo.geocaching.brouter.expressions.BExpressionContextNode;
import cgeo.geocaching.brouter.expressions.BExpressionContextWay;


final class StdModel extends OsmPathModel {
    protected BExpressionContextWay ctxWay;
    protected BExpressionContextNode ctxNode;

    public OsmPrePath createPrePath() {
        return null;
    }

    public OsmPath createPath() {
        return new StdPath();
    }

    @Override
    public void init(BExpressionContextWay expctxWay, BExpressionContextNode expctxNode, Map<String, String> keyValues) {
        ctxWay = expctxWay;
        ctxNode = expctxNode;

        BExpressionContext expctxGlobal = expctxWay; // just one of them...

    }
}
