package cgeo.geocaching.mapinterfaces;

/**
 * Defines the common functions of the provider-specific
 * MapController implementations
 * @author rsudev
 *
 */
public interface MapControllerImpl {

	void setZoom(int mapzoom);

	void setCenter(GeoPointImpl geoPoint);

	void animateTo(GeoPointImpl geoPoint);

	void zoomToSpan(int latSpanE6, int lonSpanE6);

}
