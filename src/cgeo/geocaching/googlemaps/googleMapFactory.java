package cgeo.geocaching.googlemaps;

import android.content.Context;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.mapinterfaces.CacheOverlayItemImpl;
import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapFactory;
import cgeo.geocaching.mapinterfaces.OverlayImpl;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.UserOverlayItemImpl;

public class googleMapFactory implements MapFactory{

	@Override
	public Class getMapClass() {
		return googleMapActivity.class;
	}

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
	public OverlayImpl getOverlayBaseWrapper(OverlayBase ovlIn) {
		googleOverlay baseOvl = new googleOverlay(ovlIn);
		return baseOvl;
	}
	
	@Override
	public CacheOverlayItemImpl getCacheOverlayItem(cgCoord coordinate, String type) {
		googleCacheOverlayItem baseItem = new googleCacheOverlayItem(coordinate, type);
		return baseItem;
	}

	@Override
	public UserOverlayItemImpl getUserOverlayItemBase(Context context, cgUser userOne) {
		googleUsersOverlayItem baseItem = new googleUsersOverlayItem(context, userOne);
		return baseItem;
	}

}
