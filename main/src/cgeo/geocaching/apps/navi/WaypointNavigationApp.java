package cgeo.geocaching.apps.navi;

import cgeo.geocaching.models.Waypoint;

import android.app.Activity;
import android.support.annotation.NonNull;

/**
 * interface for navigation to a waypoint
 *
 */
interface WaypointNavigationApp {
    /**
     * Navigate to the given waypoint. The caller will assert that waypoint.getCoords() is not null.
     */
    void navigate(@NonNull final Activity activity, @NonNull final Waypoint waypoint);

    boolean isEnabled(@NonNull final Waypoint waypoint);
}
