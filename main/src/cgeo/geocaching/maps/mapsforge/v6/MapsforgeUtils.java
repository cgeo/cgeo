package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.location.Geopoint;

import org.mapsforge.core.model.LatLong;

public final class MapsforgeUtils {

    private MapsforgeUtils() {
        // Do not instantiate, utility class
    }

    public static LatLong toLatLong(final Geopoint geopoint) {
        return new LatLong(geopoint.getLatitude(), geopoint.getLongitude());
    }

    public static Geopoint toGeopoint(final LatLong latLong) {
        return new Geopoint(latLong.getLatitude(), latLong.getLongitude());
    }
}
