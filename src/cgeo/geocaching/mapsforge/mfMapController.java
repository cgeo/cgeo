package cgeo.geocaching.mapsforge;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapController;

import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapControllerImpl;

public class mfMapController implements MapControllerImpl {

	private MapController mapController;
	
	public mfMapController(MapController mapControllerIn) {
		mapController = mapControllerIn;
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
		mapController.setZoom(mapzoom-1);
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

