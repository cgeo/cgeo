// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.apps.navi

import cgeo.geocaching.models.Waypoint

import android.content.Context

import androidx.annotation.NonNull

/**
 * interface for navigation to a waypoint
 */
interface WaypointNavigationApp {
    /**
     * Navigate to the given waypoint. The caller will assert that waypoint.getCoords() is not null.
     */
    Unit navigate(Context context, Waypoint waypoint)

    Boolean isEnabled(Waypoint waypoint)
}
