package cgeo.geocaching.sensors;

import cgeo.geocaching.enumerations.LocationProviderType;
import cgeo.geocaching.geopoint.Geopoint;

import android.location.Location;
import android.location.LocationManager;

public class GeoData extends Location implements IGeoData {

    public static final String INITIAL_PROVIDER = "initial";

    public GeoData(final Location location) {
        super(location);
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
        if (provider.equals("fused")) {
            return LocationProviderType.FUSED;
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

    // Some devices will not have the last position available (for example the emulator). In this case,
    // rather than waiting forever for a position update which might never come, we emulate it by placing
    // the user arbitrarly at Paris Notre-Dame, one of the most visited free tourist attractions in the world.
    public static GeoData dummyLocation() {
        final Location location = new Location(INITIAL_PROVIDER);
        location.setLatitude(48.85308);
        location.setLongitude(2.34962);
        return new GeoData(location);
    }
}
