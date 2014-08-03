package cgeo.geocaching.sensors;

import cgeo.geocaching.enumerations.LocationProviderType;
import cgeo.geocaching.geopoint.Geopoint;

import android.location.Location;

public interface IGeoData {

    public Location getLocation();
    public LocationProviderType getLocationProvider();

    public Geopoint getCoords();
    public float getBearing();
    public float getSpeed();
    public float getAccuracy();
}
