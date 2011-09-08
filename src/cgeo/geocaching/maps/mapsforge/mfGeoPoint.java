package cgeo.geocaching.maps.mapsforge;

import org.mapsforge.android.maps.GeoPoint;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;

public class mfGeoPoint extends GeoPoint implements GeoPointImpl {

	public mfGeoPoint(int latitudeE6, int longitudeE6) {
		super(latitudeE6, longitudeE6);
	}
}
