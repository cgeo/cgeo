package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Waypoint;

import android.app.Activity;

/**
 * interface for navigation to a waypoint
 *
 */
interface WaypointNavigationApp {
    void navigate(final Activity activity, final Waypoint waypoint);

    boolean isEnabled(final Waypoint waypoint);
}
