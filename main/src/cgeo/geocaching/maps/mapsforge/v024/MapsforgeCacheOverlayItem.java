package cgeo.geocaching.maps.mapsforge.v024;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;

import org.mapsforge.android.mapsold.GeoPoint;
import org.mapsforge.android.mapsold.OverlayItem;

import android.graphics.drawable.Drawable;

public class MapsforgeCacheOverlayItem extends OverlayItem implements CachesOverlayItemImpl {
    final private CacheType cacheType;
    final private IWaypoint coord;

    public MapsforgeCacheOverlayItem(IWaypoint coordinate, final CacheType type) {
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

    @Override
    public Drawable getMarker(int index) {
        return getMarker();
    }

}
