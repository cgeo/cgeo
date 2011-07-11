package cgeo.geocaching.mapsforge;

import org.mapsforge.android.maps.GeoPoint;

import cgeo.geocaching.mapinterfaces.GeoPointImpl;

public class mfGeoPoint extends GeoPoint implements GeoPointImpl {

	public mfGeoPoint(int latitudeE6, int longitudeE6) {
		super(latitudeE6, longitudeE6);
	}
}
