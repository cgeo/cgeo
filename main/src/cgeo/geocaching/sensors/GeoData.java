package cgeo.geocaching.sensors;

import cgeo.geocaching.enumerations.LocationProviderType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import javax.annotation.Nullable;

public class GeoData extends Location implements IGeoData {

    public static final String INITIAL_PROVIDER = "initial";
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
        if (isRecent(gpsLocation) || !(netLocation != null)) {
            return gpsLocation;
        }
        if (!(gpsLocation != null)) {
            return netLocation;
        }
        return gpsLocation.getTime() >= netLocation.getTime() ? gpsLocation : netLocation;
    }

    @Override
    public Location getLocation() {
        return this;
    }

    private static LocationProviderType getLocationProviderType(final String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            return LocationProviderType.GPS;
        }
        if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            return LocationProviderType.NETWORK;
        }
        // LocationManager.FUSED_PROVIDER constant is not available at API level 9
        if (provider.equals(FUSED_PROVIDER)) {
            return LocationProviderType.FUSED;
        }
        if (provider.equals(LOW_POWER_PROVIDER)) {
            return LocationProviderType.LOW_POWER;
        }
        return LocationProviderType.LAST;
    }

    @Override
    public LocationProviderType getLocationProvider() {
        return getLocationProviderType(getProvider());
    }

    @Override
    public Geopoint getCoords() {
        return new Geopoint(this);
    }

    @Nullable public static GeoData getInitialLocation(final Context context) {
        final LocationManager geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (geoManager == null) {
            Log.w("No LocationManager available");
            return null;
        }
        try {
            // Try to find a sensible initial location from the last locations known to Android.
            final Location lastGpsLocation = geoManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            final Location lastNetworkLocation = geoManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            final Location bestLocation = best(lastGpsLocation, lastNetworkLocation);
            if (bestLocation != null) {
                bestLocation.setProvider(INITIAL_PROVIDER);
                return new GeoData(bestLocation);
            }
            Log.i("No last known location available");
            return null;
        } catch (final Exception e) {
            // This error is non-fatal as its only consequence is that we will start with a dummy location
            // instead of a previously known one.
            Log.e("Error when retrieving last known location", e);
            return null;
        }
    }



    public static boolean isRecent(@Nullable final Location location) {
        return location != null && System.currentTimeMillis() <= location.getTime() + 30000;
    }

}
