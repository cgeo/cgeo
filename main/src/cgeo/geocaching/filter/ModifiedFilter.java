package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;

public class ModifiedFilter extends AbstractFilter {

    public ModifiedFilter(String name) {
        super(name);
    }

    @Override
    public boolean accepts(final cgCache cache) {
        // modified on GC
        if (cache.hasUserModifiedCoords()) {
            return true;
        }

        // or with added/modified waypoints
        for (cgWaypoint waypoint : cache.getWaypoints()) {
            if (waypoint.isUserDefined()) {
                return true;
            }
        }

        return false;
    }
}
