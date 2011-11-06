package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapFactory;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

import android.app.Activity;
import android.content.Context;

public class MapsforgeMapFactory implements MapFactory {

    @Override
    public Class<? extends Activity> getMapClass() {
        return MapsforgeMapActivity.class;
    }

    @Override
    public int getMapViewId() {
        return R.id.mfmap;
    }

    @Override
    public int getMapLayoutId() {
        return R.layout.map_mapsforge;
    }

    @Override
    public GeoPointImpl getGeoPointBase(final Geopoint coords) {
        return new MapsforgeGeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6());
    }

    @Override
    public CachesOverlayItemImpl getCachesOverlayItem(cgCoord coordinate, CacheType type) {
        MapsforgeCacheOverlayItem baseItem = new MapsforgeCacheOverlayItem(coordinate, type);
        return baseItem;
    }

    @Override
    public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context, cgUser userOne) {
        MapsforgeOtherCachersOverlayItem baseItem = new MapsforgeOtherCachersOverlayItem(context, userOne);
        return baseItem;
    }

}
