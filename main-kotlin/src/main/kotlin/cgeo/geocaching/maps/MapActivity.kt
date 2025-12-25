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

package cgeo.geocaching.maps

import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.settings.Settings

import android.os.Bundle

/**
 * This activity provides an entry point for external intent calls, and then forwards to the currently used map activity
 * implementation.
 */
class MapActivity : AbstractActivity() {
    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        startActivity(DefaultMap.getLiveMapIntent(this, Settings.getMapProvider().getMapClass()))
        finish()
    }
}
