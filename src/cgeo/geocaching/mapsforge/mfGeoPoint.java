package cgeo.geocaching.mapsforge;

import cgeo.geocaching.mapinterfaces.GeoPointImpl;

import org.mapsforge.android.maps.GeoPoint;

public class mfGeoPoint extends GeoPoint implements GeoPointImpl {

    public mfGeoPoint(int latitudeE6, int longitudeE6) {
        super(latitudeE6, longitudeE6);
    }
}
