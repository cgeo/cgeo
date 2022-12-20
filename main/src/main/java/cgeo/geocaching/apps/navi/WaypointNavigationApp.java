package cgeo.geocaching.apps.navi;

import cgeo.geocaching.models.Waypoint;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * interface for navigation to a waypoint
 */
interface WaypointNavigationApp {
    /**
     * Navigate to the given waypoint. The caller will assert that waypoint.getCoords() is not null.
     */
    void navigate(@NonNull Context context, @NonNull Waypoint waypoint);

    boolean isEnabled(@NonNull Waypoint waypoint);
}
