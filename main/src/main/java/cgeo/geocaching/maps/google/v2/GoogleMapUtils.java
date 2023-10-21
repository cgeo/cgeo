package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.pm.PackageInfoCompat;

import com.google.android.gms.common.GoogleApiAvailability;

class GoogleMapUtils {
    private GoogleMapUtils() {
        // utility class
    }

    public static boolean isGoogleMapsAvailable(final Context context) {
        // check if Google Play Services support the current Google Maps API version
        try {
            final long version = PackageInfoCompat.getLongVersionCode(context.getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0));
            if (version < GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE) {
                throw new PackageManager.NameNotFoundException("found version " + version);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("play services version too old / " + e.getMessage() + " / at least version " + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE + " required");
            return false;
        }
        return true;
    }

}
