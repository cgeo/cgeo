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
import cgeo.geocaching.location.Geopoint

import android.content.Intent

/**
 * Application for communication with the Pebble watch.
 */
class PebbleApp : AbstractRadarApp() {

    private static val INTENT: String = "com.webmajstr.pebble_gc.NAVIGATE_TO"
    private static val PACKAGE_NAME: String = "com.webmajstr.pebble_gc"

    PebbleApp() {
        super(getString(R.string.cache_menu_pebble), INTENT, PACKAGE_NAME)
    }

    override     protected Unit addCoordinates(final Intent intent, final Geopoint coords) {
        intent.putExtra(RADAR_EXTRA_LATITUDE, coords.getLatitude())
        intent.putExtra(RADAR_EXTRA_LONGITUDE, coords.getLongitude())
    }
}
