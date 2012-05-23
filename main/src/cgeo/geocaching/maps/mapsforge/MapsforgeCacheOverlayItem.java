package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;

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
