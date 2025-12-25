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

package cgeo.geocaching

import cgeo.geocaching.location.ProximityNotificationByCoords
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.settings.Settings

import android.os.Bundle

abstract class AbstractDialogFragmentWithProximityNotification : AbstractDialogFragment() {
    protected var proximityNotification: ProximityNotificationByCoords = null

    override     protected Unit onUpdateGeoData(final GeoData geo) {
        super.onUpdateGeoData(geo)
        if (null != proximityNotification) {
            proximityNotification.onUpdateGeoData(geo)
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        proximityNotification = Settings.isSpecificProximityNotificationActive() ? ProximityNotificationByCoords() : null
    }
}
