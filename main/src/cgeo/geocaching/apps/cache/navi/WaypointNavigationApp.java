package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.cgWaypoint;

import android.app.Activity;

/**
 * interface for navigation to a waypoint
 *
 */
public interface WaypointNavigationApp {
    void navigate(final Activity activity, final cgWaypoint waypoint);

    boolean isEnabled(final cgWaypoint waypoint);
}
