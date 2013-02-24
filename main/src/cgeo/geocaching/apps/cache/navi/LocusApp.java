package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.apps.AbstractLocusApp;

import android.app.Activity;

import java.util.Collections;

class LocusApp extends AbstractLocusApp implements CacheNavigationApp, WaypointNavigationApp {

    @Override
    public boolean isEnabled(Waypoint waypoint) {
        return waypoint.getCoords() != null;
    }

    @Override
    public boolean isEnabled(Geocache cache) {
        return cache.getCoords() != null;
    }

    /**
     * Show a single cache with waypoints or a single waypoint in Locus.
     * This method constructs a list of cache and waypoints only.
     *
     */
    @Override
    public void navigate(Activity activity, Waypoint waypoint) {
        showInLocus(Collections.singletonList(waypoint), true, false, activity);
    }

    @Override
    public void navigate(Activity activity, Geocache cache) {
        showInLocus(Collections.singletonList(cache), true, false, activity);
    }
}
