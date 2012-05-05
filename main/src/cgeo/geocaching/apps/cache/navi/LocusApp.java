package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractLocusApp;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;

import java.util.ArrayList;

class LocusApp extends AbstractLocusApp implements NavigationApp {

    /**
     * Show a single cache with waypoints or a single waypoint in Locus.
     * This method constructs a list of cache and waypoints only.
     *
     * @see AbstractLocusApp#showInLocus
     * @author koem
     */
    @Override
    public boolean invoke(Activity activity, cgCache cache, cgWaypoint waypoint, final Geopoint coords) {

        final ArrayList<Object> points = new ArrayList<Object>();

        // add cache if present
        if (cache != null && cache.getCoords() != null) {
            points.add(cache);
        }

        // add waypoint if present
        if (waypoint != null && waypoint.getCoords() != null) {
            points.add(waypoint);
        }

        return showInLocus(points, true, false, activity);
    }

    @Override
    public boolean isEnabled(cgWaypoint waypoint) {
        return true;
    }

}
