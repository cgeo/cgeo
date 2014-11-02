package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;

import org.mapsforge.core.GeoPoint;

public class MapsforgeGeoPoint extends GeoPoint implements GeoPointImpl {

    private static final long serialVersionUID = 1L;

    public MapsforgeGeoPoint(int latitudeE6, int longitudeE6) {
        super(latitudeE6, longitudeE6);
    }

    @Override
    public Geopoint getCoords() {
        return new Geopoint(getLatitudeE6() / 1e6, getLongitudeE6() / 1e6);
    }

    @Override
    public int getLatitudeE6() {
        return latitudeE6;
    }

    @Override
    public int getLongitudeE6() {
        return longitudeE6;
    }
}
