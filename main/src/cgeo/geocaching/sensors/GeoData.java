package cgeo.geocaching.sensors;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

public class GeoData extends Location {

    public static final String INITIAL_PROVIDER = "initial";
    public static final String HOME_PROVIDER = "home";
    public static final String FUSED_PROVIDER = "fused";
    public static final String LOW_POWER_PROVIDER = "low-power";

    // Some devices will not have the last position available (for example the emulator). In this case,
    // rather than waiting forever for a position update which might never come, we emulate it by placing
    // the user arbitrarly at Paris Notre-Dame, one of the most visited free tourist attractions in the world.
    final public static GeoData DUMMY_LOCATION = new GeoData(new Location(INITIAL_PROVIDER));

    static {
        DUMMY_LOCATION.setLatitude(48.85308);
        DUMMY_LOCATION.setLongitude(2.34962);
    }

    public GeoData(final Location location) {
        super(location);
    }

    @Nullable
    static Location best(@Nullable final Location gpsLocation, @Nullable final Location netLocation) {
        if (netLocation == null || (gpsLocation != null && System.currentTimeMillis() <= gpsLocation.getTime() + 30000)) {
            return gpsLocation;
        }
        if (gpsLocation == null) {
            return netLocation;
        }
        return gpsLocation.getTime() >= netLocation.getTime() ? gpsLocation : netLocation;
    }

    public LocationProviderType getLocationProvider() {
        switch (getProvider()) {
            case LocationManager.GPS_PROVIDER:
                return LocationProviderType.GPS;
            case LocationManager.NETWORK_PROVIDER:
                return LocationProviderType.NETWORK;
            case FUSED_PROVIDER:
                // LocationManager.FUSED_PROVIDER constant is not available at API level 9
                return LocationProviderType.FUSED;
            case LOW_POWER_PROVIDER:
                return LocationProviderType.LOW_POWER;
            case HOME_PROVIDER:
                return LocationProviderType.HOME;
            default:
                return LocationProviderType.LAST;
        }
    }

    @NonNull
    public Geopoint getCoords() {
        return new Geopoint(this);
    }

    @Nullable
    public static GeoData getInitialLocation(final Context context) {
        final LocationManager geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (geoManager != null) {
            try {
                // Try to find a sensible initial location from the last locations known to Android.
                final Location lastGpsLocation = geoManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                final Location lastNetworkLocation = geoManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                final Location bestLocation = best(lastGpsLocation, lastNetworkLocation);
                if (bestLocation != null) {
                    bestLocation.setProvider(INITIAL_PROVIDER);
                    return new GeoData(bestLocation);
                }
            } catch (final Exception e) {
                // This error is non-fatal as its only consequence is that we will start with a dummy location
                // instead of a previously known one.
                Log.e("Error when retrieving last known location", e);
            }
        } else {
            Log.w("No LocationManager available");
        }
        final String homeLocationStr = Settings.getHomeLocation();
        if (StringUtils.isNotBlank(homeLocationStr)) {
            try {
                assert (homeLocationStr != null);
                final Geopoint homeLocation = new Geopoint(homeLocationStr);
                Log.i("No last known location available, using home location");
                final Location initialLocation = new Location(HOME_PROVIDER);
                initialLocation.setLatitude(homeLocation.getLatitude());
                initialLocation.setLongitude(homeLocation.getLongitude());
                return new GeoData(initialLocation);
            } catch (final Geopoint.ParseException e) {
                Log.w("Unable to parse home location " + homeLocationStr, e);
            }
        }
        Log.i("No last known location nor home location available");
        return null;
    }

}