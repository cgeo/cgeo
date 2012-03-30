package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;

import org.mapsforge.core.GeoPoint;

public class MapsforgeGeoPoint extends GeoPoint implements GeoPointImpl {

    public MapsforgeGeoPoint(int latitudeE6, int longitudeE6) {
        super(latitudeE6, longitudeE6);
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
