package cgeo.geocaching.maps.interfaces;

/**
 * Defines the common functions of the provider-specific
 * MapController implementations
 */
public interface MapControllerImpl {

    void setZoom(int mapzoom);

    void setCenter(GeoPointImpl geoPoint);

    void animateTo(GeoPointImpl geoPoint);

    void zoomToSpan(int latSpanE6, int lonSpanE6);

}
