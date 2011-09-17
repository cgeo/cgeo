package cgeo.geocaching.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.mapinterfaces.CacheOverlayItemImpl;
import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapFactory;
import cgeo.geocaching.mapinterfaces.UserOverlayItemImpl;

import android.app.Activity;
import android.content.Context;

public class mfMapFactory implements MapFactory {

    @Override
    public Class<? extends Activity> getMapClass() {
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
    public GeoPointImpl getGeoPointBase(final Geopoint coords) {
        return new mfGeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6());
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
