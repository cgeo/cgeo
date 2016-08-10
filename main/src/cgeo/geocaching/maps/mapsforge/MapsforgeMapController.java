package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;

import org.mapsforge.v3.android.maps.MapController;
import org.mapsforge.v3.core.GeoPoint;

public class MapsforgeMapController implements MapControllerImpl {

    private final MapController mapController;
    private final int maxZoomLevel;

    public MapsforgeMapController(final MapController mapControllerIn, final int maxZoomLevelIn) {
        mapController = mapControllerIn;
        maxZoomLevel = maxZoomLevelIn;
    }

    @Override
    public void animateTo(final GeoPointImpl geoPoint) {
        mapController.setCenter(castToGeoPoint(geoPoint));
    }

    private static GeoPoint castToGeoPoint(final GeoPointImpl geoPoint) {
        return (GeoPoint) geoPoint;
    }

    @Override
    public void setCenter(final GeoPointImpl geoPoint) {
        mapController.setCenter(castToGeoPoint(geoPoint));
    }

    /**
     * Set the map zoom level to mapzoom-1 or maxZoomLevel, whichever is least
     * mapzoom-1 is used to be compatible with Google Maps zoom levels
     */
    @Override
    public void setZoom(final int mapzoom) {
        // Google Maps and OSM Maps use different zoom levels for the same view.
        // All OSM Maps zoom levels are offset by 1 so they match Google Maps.
        mapController.setZoom(Math.min(mapzoom + 1, maxZoomLevel));
    }

    @Override
    public void zoomToSpan(final int latSpanE6, final int lonSpanE6) {

        if (latSpanE6 != 0 && lonSpanE6 != 0) {
            // calculate zoomlevel
            final int distDegree = Math.max(latSpanE6, lonSpanE6);
            final int zoomLevel = (int) Math.floor(Math.log(360.0 * 1e6 / distDegree) / Math.log(2));
            mapController.setZoom(zoomLevel + 1);
        }
    }
}
