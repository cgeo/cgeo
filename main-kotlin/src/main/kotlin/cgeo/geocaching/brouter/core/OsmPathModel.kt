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

abstract class OsmPathModel {
    public abstract OsmPrePath createPrePath()

    public abstract OsmPath createPath()

    public abstract Unit init(BExpressionContextWay expctxWay, BExpressionContextNode expctxNode, Map<String, String> keyValues)
}
