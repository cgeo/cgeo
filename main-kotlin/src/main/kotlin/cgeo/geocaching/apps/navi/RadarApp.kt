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

class RadarApp : AbstractRadarApp() {

    private static val INTENT: String = "com.google.android.radar.SHOW_RADAR"
    private static val PACKAGE_NAME: String = "com.eclipsim.gpsstatus2"

    RadarApp() {
        super(getString(R.string.cache_menu_radar), INTENT, PACKAGE_NAME)
    }

    override     protected Unit addCoordinates(final Intent intent, final Geopoint coords) {
        intent.putExtra(RADAR_EXTRA_LATITUDE, (Float) coords.getLatitude())
        intent.putExtra(RADAR_EXTRA_LONGITUDE, (Float) coords.getLongitude())
    }

}
