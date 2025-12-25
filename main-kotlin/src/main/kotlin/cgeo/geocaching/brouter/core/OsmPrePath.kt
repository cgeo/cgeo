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
 * Simple version of OsmPath just to get angle and priority of first segment
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.mapaccess.OsmLink
import cgeo.geocaching.brouter.mapaccess.OsmNode

abstract class OsmPrePath {
    public OsmPrePath next
    protected OsmNode sourceNode
    protected OsmNode targetNode
    protected OsmLink link

    public Unit init(final OsmPath origin, final OsmLink link, final RoutingContext rc) {
        this.link = link
        this.sourceNode = origin.getTargetNode()
        this.targetNode = link.getTarget(sourceNode)
        initPrePath(origin, rc)
    }

    protected abstract Unit initPrePath(OsmPath origin, RoutingContext rc)
}
