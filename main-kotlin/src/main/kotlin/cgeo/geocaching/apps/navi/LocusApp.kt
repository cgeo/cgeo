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

import cgeo.geocaching.R
import cgeo.geocaching.apps.AbstractLocusApp
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint

import android.content.Context
import android.content.Intent

import androidx.annotation.NonNull

import java.util.Collections

class LocusApp : AbstractLocusApp() : CacheNavigationApp, WaypointNavigationApp {

    private static val INTENT: String = Intent.ACTION_VIEW

    protected LocusApp() {
        super(getString(R.string.caches_map_locus), INTENT)
    }

    override     public Boolean isEnabled(final Waypoint waypoint) {
        return waypoint.getCoords() != null
    }

    override     public Boolean isEnabled(final Geocache cache) {
        return cache.getCoords() != null
    }

    /**
     * Show a single cache with waypoints or a single waypoint in Locus.
     * This method constructs a list of cache and waypoints only.
     */
    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        showInLocus(Collections.singletonList(waypoint), true, false, context)
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        showInLocus(Collections.singletonList(cache), true, false, context)
    }
}
