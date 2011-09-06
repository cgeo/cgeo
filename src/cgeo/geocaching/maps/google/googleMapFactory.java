package cgeo.geocaching.maps.google;

import android.content.Context;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapFactory;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

public class googleMapFactory implements MapFactory{

	@Override
	public int getMapViewId() {
		return R.id.map;
	}

	@Override
	public int getMapLayoutId() {
		return R.layout.googlemap;
	}

	@Override
	public GeoPointImpl getGeoPointBase(int latE6, int lonE6) {
		return new googleGeoPoint(latE6, lonE6);
	}

	@Override
	public CachesOverlayItemImpl getCacheOverlayItem(cgCoord coordinate, String type) {
		googleCacheOverlayItem baseItem = new googleCacheOverlayItem(coordinate, type);
		return baseItem;
	}

	@Override
	public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context, cgUser userOne) {
		googleUsersOverlayItem baseItem = new googleUsersOverlayItem(context, userOne);
		return baseItem;
	}
}
