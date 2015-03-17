package cgeo.geocaching.maps.google.v1;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

public class GoogleMapController implements MapControllerImpl {

    private final MapController mapController;

    public GoogleMapController(final MapController mapControllerIn) {
        mapController = mapControllerIn;
    }

    @Override
    public void animateTo(final GeoPointImpl geoPoint) {
        mapController.animateTo(castToGeoPointImpl(geoPoint));
    }

    private static GeoPoint castToGeoPointImpl(final GeoPointImpl geoPoint) {
        assert geoPoint instanceof GeoPoint;
        return (GeoPoint) geoPoint;
    }

    @Override
    public void setCenter(final GeoPointImpl geoPoint) {
        mapController.setCenter(castToGeoPointImpl(geoPoint));
    }

    @Override
    public void setZoom(final int mapzoom) {
        mapController.setZoom(mapzoom);
    }

    @Override
    public void zoomToSpan(final int latSpanE6, final int lonSpanE6) {
        mapController.zoomToSpan(latSpanE6, lonSpanE6);
    }

}
