package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;

import org.mapsforge.core.model.LatLong;

public class MapsforgeGeoPoint implements GeoPointImpl {

    private final LatLong latLong;

    public MapsforgeGeoPoint(final LatLong latLong) {
        this.latLong = latLong;
    }

    @Override
    public Geopoint getCoords() {
        return new Geopoint(latLong.latitude, latLong.longitude);
    }

    @Override
    public int getLatitudeE6() {
        return (int) (latLong.latitude * 1e6);
    }

    @Override
    public int getLongitudeE6() {
        return (int) (latLong.longitude * 1e6);
    }

}
