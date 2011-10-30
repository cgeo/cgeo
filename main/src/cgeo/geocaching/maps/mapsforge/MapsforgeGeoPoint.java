package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;

import org.mapsforge.android.maps.GeoPoint;

public class MapsforgeGeoPoint extends GeoPoint implements GeoPointImpl {

    public MapsforgeGeoPoint(int latitudeE6, int longitudeE6) {
        super(latitudeE6, longitudeE6);
    }
}
