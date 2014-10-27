package cgeo.geocaching.maps.google.v1;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

public class GoogleMapController implements MapControllerImpl {

    private MapController mapController;

    public GoogleMapController(MapController mapControllerIn) {
        mapController = mapControllerIn;
    }

    @Override
    public void animateTo(GeoPointImpl geoPoint) {
        mapController.animateTo(castToGeoPointImpl(geoPoint));
    }

    private static GeoPoint castToGeoPointImpl(GeoPointImpl geoPoint) {
        assert geoPoint instanceof GeoPoint;
        return (GeoPoint) geoPoint;
    }

    @Override
    public void setCenter(GeoPointImpl geoPoint) {
        mapController.setCenter(castToGeoPointImpl(geoPoint));
    }

    @Override
    public void setZoom(int mapzoom) {
        mapController.setZoom(mapzoom);
    }

    @Override
    public void zoomToSpan(int latSpanE6, int lonSpanE6) {
        mapController.zoomToSpan(latSpanE6, lonSpanE6);
    }

}
