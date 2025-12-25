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

package cgeo.geocaching.maps.google.v2

import cgeo.geocaching.utils.Log

import android.content.Context
import android.content.pm.PackageManager

import androidx.core.content.pm.PackageInfoCompat

import com.google.android.gms.common.GoogleApiAvailability

class GoogleMapUtils {
    private GoogleMapUtils() {
        // utility class
    }

    public static Boolean isGoogleMapsAvailable(final Context context) {
        // check if Google Play Services support the current Google Maps API version
        try {
            val version: Long = PackageInfoCompat.getLongVersionCode(context.getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0))
            if (version < GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE) {
                throw PackageManager.NameNotFoundException("found version " + version)
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("play services version too old / " + e.getMessage() + " / at least version " + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE + " required")
            return false
        }
        return true
    }

}
