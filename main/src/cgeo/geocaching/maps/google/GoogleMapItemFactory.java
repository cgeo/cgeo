package cgeo.geocaching.maps.google;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.go4cache.Go4CacheUser;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

import android.content.Context;

public class GoogleMapItemFactory implements MapItemFactory {

    @Override
    public GeoPointImpl getGeoPointBase(final Geopoint coords) {
        return new GoogleGeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6());
    }

    @Override
    public CachesOverlayItemImpl getCachesOverlayItem(final IWaypoint coordinate, final CacheType type) {
        GoogleCacheOverlayItem baseItem = new GoogleCacheOverlayItem(coordinate, type);
        return baseItem;
    }

    @Override
    public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context, Go4CacheUser userOne) {
        GoogleOtherCachersOverlayItem baseItem = new GoogleOtherCachersOverlayItem(context, userOne);
        return baseItem;
    }
}
