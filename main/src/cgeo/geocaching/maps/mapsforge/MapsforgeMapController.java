package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;

import org.mapsforge.android.maps.MapController;
import org.mapsforge.core.GeoPoint;

public class MapsforgeMapController implements MapControllerImpl {

    private MapController mapController;
    private int maxZoomLevel;

    public MapsforgeMapController(MapController mapControllerIn, int maxZoomLevelIn) {
        mapController = mapControllerIn;
        maxZoomLevel = maxZoomLevelIn;
    }

    @Override
    public void animateTo(GeoPointImpl geoPoint) {
        mapController.setCenter((GeoPoint) geoPoint);
    }

    @Override
    public void setCenter(GeoPointImpl geoPoint) {
        mapController.setCenter((GeoPoint) geoPoint);
    }

    @Override
    public void setZoom(int mapzoom) {
        mapController.setZoom(mapzoom > maxZoomLevel ? maxZoomLevel : mapzoom);
    }

    @Override
    public void zoomToSpan(int latSpanE6, int lonSpanE6) {

        if (latSpanE6 != 0 && lonSpanE6 != 0) {
            // calculate zoomlevel
            int distDegree = Math.max(latSpanE6, lonSpanE6);
            int zoomLevel = (int) Math.floor(Math.log(360.0 * 1e6 / distDegree) / Math.log(2));
            mapController.setZoom(zoomLevel);
        }
    }
}
