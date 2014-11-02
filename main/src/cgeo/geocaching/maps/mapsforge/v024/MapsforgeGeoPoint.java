package cgeo.geocaching.maps.mapsforge.v024;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;

import org.mapsforge.android.mapsold.GeoPoint;

public class MapsforgeGeoPoint extends GeoPoint implements GeoPointImpl {

    public MapsforgeGeoPoint(int latitudeE6, int longitudeE6) {
        super(latitudeE6, longitudeE6);
    }

    @Override
    public Geopoint getCoords() {
        return new Geopoint(getLatitudeE6() / 1e6, getLongitudeE6() / 1e6);
    }
}
