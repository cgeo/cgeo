package cgeo.geocaching.sensors;

import cgeo.geocaching.enumerations.LocationProviderType;
import cgeo.geocaching.geopoint.Geopoint;

import android.location.Location;
import android.location.LocationManager;

class GeoData extends Location implements IGeoData {

    GeoData(final Location location) {
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
}
