package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;

import com.google.android.gms.maps.model.LatLng;
import org.oscim.core.GeoPoint;

public class GoogleGeoPoint implements GeoPointImpl {

    protected final int latE6, longE6;

    public GoogleGeoPoint(final int latitudeE6, final int longitudeE6) {
        latE6 = latitudeE6;
        longE6 = longitudeE6;
    }

    public GoogleGeoPoint(final LatLng latlng) {
        this((int) (latlng.latitude * 1e6), (int) (latlng.longitude * 1e6));
    }

    public GoogleGeoPoint(final GeoPoint center) {
        this(center.latitudeE6, center.longitudeE6);
    }

    @Override
    public Geopoint getCoords() {
        return new Geopoint(getLatitudeE6() / 1e6, getLongitudeE6() / 1e6);
    }

    @Override
    public int getLatitudeE6() {
        return latE6;
    }

    @Override
    public int getLongitudeE6() {
        return longE6;
    }
}
