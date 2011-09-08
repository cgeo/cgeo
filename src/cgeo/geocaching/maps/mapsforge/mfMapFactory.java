package cgeo.geocaching.maps.mapsforge;

import android.content.Context;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapFactory;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

public class mfMapFactory implements MapFactory{

	@Override
	public int getMapViewId() {
		return R.id.mfmap;
	}

	@Override
	public int getMapLayoutId() {
		return R.layout.mfmap;
	}

	@Override
	public GeoPointImpl getGeoPointBase(int latE6, int lonE6) {
		return new mfGeoPoint(latE6, lonE6);
	}

	@Override
	public CachesOverlayItemImpl getCacheOverlayItem(cgCoord coordinate, String type) {
		mfCacheOverlayItem baseItem = new mfCacheOverlayItem(coordinate, type);
		return baseItem;
	}

	@Override
	public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context, cgUser userOne) {
		mfUsersOverlayItem baseItem = new mfUsersOverlayItem(context, userOne);
		return baseItem;
	}

}
