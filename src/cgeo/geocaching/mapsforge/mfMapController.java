package cgeo.geocaching.mapsforge;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapController;

import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapControllerImpl;

public class mfMapController implements MapControllerImpl {

	private MapController mapController;
	private int maxZoomLevel;
	
	public mfMapController(MapController mapControllerIn, int maxZoomLevelIn) {
		mapController = mapControllerIn;
		maxZoomLevel = maxZoomLevelIn;
	}

	@Override
	public void animateTo(GeoPointImpl geoPoint) {
		mapController.setCenter((GeoPoint)geoPoint);
	}

	@Override
	public void setCenter(GeoPointImpl geoPoint) {
		mapController.setCenter((GeoPoint)geoPoint);
	}

	@Override
	public void setZoom(int mapzoom) {
		int mfzoom = mapzoom-1;
		if (mfzoom > maxZoomLevel) {
			mfzoom = maxZoomLevel;
		}
		mapController.setZoom(mfzoom);
	}

	@Override
	public void zoomToSpan(int latSpanE6, int lonSpanE6) {
		
		if (latSpanE6 != 0 && lonSpanE6 != 0) {
			// calculate zoomlevel
			int distDegree = Math.max(latSpanE6, lonSpanE6);
			int zoomLevel = (int) Math.floor(Math.log(360.0*1e6/distDegree)/Math.log(2));
			mapController.setZoom(zoomLevel+1);
			}		
	}	
}

