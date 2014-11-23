package cgeo.geocaching.sensors;

import cgeo.geocaching.location.Geopoint;

import org.eclipse.jdt.annotation.NonNull;

import android.location.Location;

public interface IGeoData {

    public Location getLocation();
    public LocationProviderType getLocationProvider();

    @NonNull public Geopoint getCoords();
    public float getBearing();
    public float getSpeed();
    public float getAccuracy();
}
