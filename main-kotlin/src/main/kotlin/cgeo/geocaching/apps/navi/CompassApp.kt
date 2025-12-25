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

import cgeo.geocaching.CompassActivity
import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint

import android.content.Context

import androidx.annotation.NonNull

class CompassApp : AbstractPointNavigationApp() {

    CompassApp() {
        super(getString(R.string.compass_title), null)
    }

    override     public Boolean isInstalled() {
        return true
    }

    override     public Unit navigate(final Context context, final Geopoint coords) {
        CompassActivity.startActivityPoint(context, coords, getString(R.string.navigation_direct_navigation))
    }

    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        CompassActivity.startActivityWaypoint(context, waypoint)
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        CompassActivity.startActivityCache(context, cache)
    }

}
