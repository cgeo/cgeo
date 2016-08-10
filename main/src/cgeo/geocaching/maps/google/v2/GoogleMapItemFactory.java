package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.models.IWaypoint;

public class GoogleMapItemFactory implements MapItemFactory {

    private final BitmapDescriptorCache bitmapDescriptorCache = new BitmapDescriptorCache();

    @Override
    public GeoPointImpl getGeoPointBase(final Geopoint coords) {
        return new GoogleGeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6());
    }

    @Override
    public CachesOverlayItemImpl getCachesOverlayItem(final IWaypoint coordinate, final boolean applyDistanceRule) {
        final GoogleCacheOverlayItem item = new GoogleCacheOverlayItem(coordinate, applyDistanceRule);
        item.setBitmapDescriptorCache(bitmapDescriptorCache);
        return item;
    }
}
