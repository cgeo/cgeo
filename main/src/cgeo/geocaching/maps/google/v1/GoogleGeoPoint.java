package cgeo.geocaching.maps.google.v1;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;

import com.google.android.maps.GeoPoint;

public class GoogleGeoPoint extends GeoPoint implements GeoPointImpl {

    public GoogleGeoPoint(int latitudeE6, int longitudeE6) {
        super(latitudeE6, longitudeE6);
    }

    @Override
    public Geopoint getCoords() {
        return new Geopoint(getLatitudeE6() / 1e6, getLongitudeE6() / 1e6);
    }

}
