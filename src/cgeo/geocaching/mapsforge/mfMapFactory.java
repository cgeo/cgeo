package cgeo.geocaching.mapsforge;

import android.content.Context;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.mapinterfaces.CacheOverlayItemImpl;
import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapFactory;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.OverlayImpl;
import cgeo.geocaching.mapinterfaces.UserOverlayItemImpl;

public class mfMapFactory implements MapFactory{

	@Override
	public Class getMapClass() {
		return mfMapActivity.class;
	}

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
	public OverlayImpl getOverlayBaseWrapper(OverlayBase ovlIn) {
		mfOverlay baseOvl = new mfOverlay(ovlIn);
		return baseOvl;
	}
	
	@Override
	public CacheOverlayItemImpl getCacheOverlayItem(cgCoord coordinate, String type) {
		mfCacheOverlayItem baseItem = new mfCacheOverlayItem(coordinate, type);
		return baseItem;
	}

	@Override
	public UserOverlayItemImpl getUserOverlayItemBase(Context context, cgUser userOne) {
		mfUsersOverlayItem baseItem = new mfUsersOverlayItem(context, userOne);
		return baseItem;
	}

}
