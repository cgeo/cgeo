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

package cgeo.geocaching.playservices

import cgeo.geocaching.CgeoApplication

import android.content.Context

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class GooglePlayServices {

    private static Boolean isGooglePlayServicesAvailable = false
    private static Boolean initialized = false

    private GooglePlayServices() {
        // utility class
    }

    /**
     * Check if Google Play services is available on this device
     */
    public static Boolean isAvailable() {
        if (!initialized) {
            initialize()
        }
        return isGooglePlayServicesAvailable
    }

    /**
     * cache the result of querying for play services
     */
    private static Unit initialize() {
        val context: Context = CgeoApplication.getInstance()
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
            isGooglePlayServicesAvailable = true
        }
        initialized = true
    }
}
