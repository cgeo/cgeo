package cgeo.geocaching.googlemaps;

import android.content.Context;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.mapinterfaces.CacheOverlayItemImpl;
import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapFactory;
import cgeo.geocaching.mapinterfaces.UserOverlayItemImpl;

import com.google.android.maps.MapActivity;

public class googleMapFactory implements MapFactory{

	@Override
	public Class<?extends MapActivity> getMapClass() {
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
	public GeoPointImpl getGeoPointBase(final Geopoint coords) {
		return new googleGeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6());
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
