package cgeo.geocaching.sensors;

import cgeo.geocaching.enumerations.LocationProviderType;
import cgeo.geocaching.geopoint.Geopoint;

import android.location.Location;
import android.location.LocationManager;

class GeoData extends Location implements IGeoData {
    public final boolean gpsEnabled;
    public final int satellitesVisible;
    public final int satellitesFixed;
    public final boolean pseudoLocation;

    GeoData(final Location location, final boolean gpsEnabled, final int satellitesVisible, final int satellitesFixed, final boolean pseudoLocation) {
        super(location);
        this.gpsEnabled = gpsEnabled;
        this.satellitesVisible = satellitesVisible;
        this.satellitesFixed = satellitesFixed;
        this.pseudoLocation = pseudoLocation;
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

    @Override
    public boolean getGpsEnabled() {
        return gpsEnabled;
    }

    @Override
    public int getSatellitesVisible() {
        return satellitesVisible;
    }

    @Override
    public int getSatellitesFixed() {
        return satellitesFixed;
    }

    @Override
    public boolean isPseudoLocation() {
        return pseudoLocation;
    }
}
