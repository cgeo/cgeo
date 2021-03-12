/**
 * Simple version of OsmPath just to get angle and priority of first segment
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.mapaccess.OsmLink;
import cgeo.geocaching.brouter.mapaccess.OsmNode;

public abstract class OsmPrePath {
    public OsmPrePath next;
    protected OsmNode sourceNode;
    protected OsmNode targetNode;
    protected OsmLink link;

    public void init(final OsmPath origin, final OsmLink link, final RoutingContext rc) {
        this.link = link;
        this.sourceNode = origin.getTargetNode();
        this.targetNode = link.getTarget(sourceNode);
        initPrePath(origin, rc);
    }

    protected abstract void initPrePath(OsmPath origin, RoutingContext rc);
}
