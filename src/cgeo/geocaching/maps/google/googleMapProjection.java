package cgeo.geocaching.maps.google;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

import android.graphics.Point;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;

public class googleMapProjection implements MapProjectionImpl {
	
	private Projection projection;

	public googleMapProjection(Projection projectionIn) {
		projection = projectionIn;
	}

	@Override
	public void toPixels(GeoPointImpl leftGeo, Point left) {
		projection.toPixels((GeoPoint) leftGeo, left);
	}

	@Override
	public Object getImpl() {
		return projection;
	}

}
