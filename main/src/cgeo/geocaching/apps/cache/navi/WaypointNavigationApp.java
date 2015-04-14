package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Waypoint;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

/**
 * interface for navigation to a waypoint
 *
 */
interface WaypointNavigationApp {
    void navigate(@NonNull final Activity activity, @NonNull final Waypoint waypoint);

    boolean isEnabled(@NonNull final Waypoint waypoint);
}
