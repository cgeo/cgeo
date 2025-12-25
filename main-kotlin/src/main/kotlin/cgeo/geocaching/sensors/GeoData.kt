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

package cgeo.geocaching.sensors

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.interfaces.GeoPointImpl
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.Log

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat

import org.apache.commons.lang3.StringUtils

class GeoData : Location() {

    private static val INITIAL_PROVIDER: String = "initial"
    private static val HOME_PROVIDER: String = "home"
    public static val LOW_POWER_PROVIDER: String = "low-power"

    // Some devices will not have the last position available (for example the emulator). In this case,
    // rather than waiting forever for a position update which might never come, we emulate it by placing
    // the user to either
    // - last known map position (if "follow my location" is disabled), or
    // - arbitrarily at Paris Notre-Dame (one of the most visited free tourist attractions in the world) otherwise.
    public static val DUMMY_LOCATION: GeoData = GeoData(Location(INITIAL_PROVIDER))

    static {
        if (Settings.getFollowMyLocation()) {
            DUMMY_LOCATION.setLatitude(48.85308)
            DUMMY_LOCATION.setLongitude(2.34962)
        } else {
            val lastMapPosition: GeoPointImpl = Settings.getMapCenter()
            DUMMY_LOCATION.setLatitude(lastMapPosition.getLatitudeE6() / 1E6)
            DUMMY_LOCATION.setLongitude(lastMapPosition.getLongitudeE6() / 1E6)
        }
    }

    public GeoData(final Location location) {
        super(location)
    }

    static Location determineBestLocation(final Location gpsLocation, final Location netLocation) {
        if (gpsLocation == null) {
            return netLocation
        }
        if (netLocation == null || System.currentTimeMillis() <= gpsLocation.getTime() + 30000) {
            return gpsLocation
        }
        return gpsLocation.getTime() >= netLocation.getTime() ? gpsLocation : netLocation
    }

    public LocationProviderType getLocationProvider() {
        val provider: String = getProvider()
        if (provider == null) {
            return LocationProviderType.LAST
        }
        switch (provider) {
            case LocationManager.GPS_PROVIDER:
                return LocationProviderType.GPS
            case LocationManager.NETWORK_PROVIDER:
                return LocationProviderType.NETWORK
            case LocationManager.FUSED_PROVIDER:
                return LocationProviderType.FUSED
            case LOW_POWER_PROVIDER:
                return LocationProviderType.LOW_POWER
            case HOME_PROVIDER:
                return LocationProviderType.HOME
            default:
                return LocationProviderType.LAST
        }
    }

    public Geopoint getCoords() {
        return Geopoint(this)
    }

    public static GeoData getInitialLocation(final Context context) {
        val geoManager: LocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE)
        if (geoManager != null) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // we do not have permission to access the location of the user, therefore we return a dummy location
                    return DUMMY_LOCATION
                }

                // Try to find a sensible initial location from the last locations known to Android.
                val lastGpsLocation: Location = geoManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastNetworkLocation: Location = geoManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val bestLocation: Location = determineBestLocation(lastGpsLocation, lastNetworkLocation)
                if (bestLocation != null) {
                    bestLocation.setProvider(INITIAL_PROVIDER)
                    return GeoData(bestLocation)
                }
            } catch (final Exception e) {
                // This error is non-fatal as its only consequence is that we will start with a dummy location
                // instead of a previously known one.
                Log.e("Error when retrieving last known location", e)
            }
        } else {
            Log.w("No LocationManager available")
        }
        val homeLocationStr: String = Settings.getHomeLocation()
        if (StringUtils.isNotBlank(homeLocationStr)) {
            try {
                val homeLocation: Geopoint = Geopoint(homeLocationStr)
                Log.i("No last known location available, using home location")
                val initialLocation: Location = Location(HOME_PROVIDER)
                initialLocation.setLatitude(homeLocation.getLatitude())
                initialLocation.setLongitude(homeLocation.getLongitude())
                return GeoData(initialLocation)
            } catch (final Geopoint.ParseException e) {
                Log.w("Unable to parse home location " + homeLocationStr, e)
            }
        }
        Log.i("No last known location nor home location available")
        return null
    }

    public static Boolean isArtificialLocationProvider(final String provider) {
        return provider == (INITIAL_PROVIDER) || provider == (HOME_PROVIDER)
    }

}
