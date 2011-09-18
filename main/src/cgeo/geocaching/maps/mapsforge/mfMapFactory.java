package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapFactory;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

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
    public CachesOverlayItemImpl getCachesOverlayItem(cgCoord coordinate, String type) {
        mfCacheOverlayItem baseItem = new mfCacheOverlayItem(coordinate, type);
        return baseItem;
    }

    @Override
    public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context, cgUser userOne) {
        mfOtherCachersOverlayItem baseItem = new mfOtherCachersOverlayItem(context, userOne);
        return baseItem;
    }

}
