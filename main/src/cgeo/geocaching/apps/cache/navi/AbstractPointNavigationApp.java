package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractApp;

import android.app.Activity;

/**
 * navigation app for simple point navigation (no differentiation between cache/waypoint/point)
 */
abstract class AbstractPointNavigationApp extends AbstractApp implements CacheNavigationApp, WaypointNavigationApp, GeopointNavigationApp {

    protected AbstractPointNavigationApp(String name, String intent) {
        super(name, intent);
    }

    protected AbstractPointNavigationApp(String name, String intent, String packageName) {
        super(name, intent, packageName);
    }

    @Override
    public void navigate(Activity activity, cgCache cache) {
        navigate(activity, cache.getCoords());
    }

    @Override
    public void navigate(Activity activity, cgWaypoint waypoint) {
        navigate(activity, waypoint.getCoords());
    }

    @Override
    public boolean isEnabled(cgCache cache) {
        return cache.getCoords() != null;
    }

    @Override
    public boolean isEnabled(cgWaypoint waypoint) {
        return waypoint.getCoords() != null;
    }
}
