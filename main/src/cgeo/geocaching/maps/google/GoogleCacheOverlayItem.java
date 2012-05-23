package cgeo.geocaching.maps.google;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class GoogleCacheOverlayItem extends OverlayItem implements CachesOverlayItemImpl {
    final private CacheType cacheType;
    final private IWaypoint coord;

    public GoogleCacheOverlayItem(final IWaypoint coordinate, final CacheType type) {
        super(new GeoPoint(coordinate.getCoords().getLatitudeE6(), coordinate.getCoords().getLongitudeE6()), coordinate.getName(), "");

        this.cacheType = type;
        this.coord = coordinate;
    }

    @Override
    public IWaypoint getCoord() {
        return coord;
    }

    @Override
    public CacheType getType() {
        return cacheType;
    }

}
