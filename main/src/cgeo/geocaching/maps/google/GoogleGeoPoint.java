package cgeo.geocaching.maps.google;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;

import com.google.android.maps.GeoPoint;

public class GoogleGeoPoint extends GeoPoint implements GeoPointImpl {

    public GoogleGeoPoint(int latitudeE6, int longitudeE6) {
        super(latitudeE6, longitudeE6);
    }

}
