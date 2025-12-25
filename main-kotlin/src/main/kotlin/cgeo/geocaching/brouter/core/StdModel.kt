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

/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.expressions.BExpressionContextNode
import cgeo.geocaching.brouter.expressions.BExpressionContextWay

import java.util.Map

class StdModel : OsmPathModel() {
    private BExpressionContextWay ctxWay
    private BExpressionContextNode ctxNode

    public OsmPrePath createPrePath() {
        return null
    }

    public OsmPath createPath() {
        return StdPath()
    }

    override     public Unit init(final BExpressionContextWay expctxWay, final BExpressionContextNode expctxNode, final Map<String, String> keyValues) {
        ctxWay = expctxWay
        ctxNode = expctxNode
    }
}
