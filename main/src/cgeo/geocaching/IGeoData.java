package cgeo.geocaching;

import cgeo.geocaching.enumerations.LocationProviderType;
import cgeo.geocaching.geopoint.Geopoint;

import android.location.Location;

public interface IGeoData {

    public Location getLocation();
    public LocationProviderType getLocationProvider();
    public Geopoint getCoordsNow();
    public Double getAltitudeNow();
    public float getBearingNow();
    public float getSpeedNow();
    public float getAccuracyNow();
    public int getSatellitesVisible();
    public int getSatellitesFixed();

}
