package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.cgCoord;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;

import android.graphics.drawable.Drawable;

public class MapsforgeCacheOverlayItem extends OverlayItem implements CachesOverlayItemImpl {
    final private CacheType cacheType;
    final private cgCoord coord;

    public MapsforgeCacheOverlayItem(cgCoord coordinate, final CacheType type) {
        super(new GeoPoint(coordinate.getCoords().getLatitudeE6(), coordinate.getCoords().getLongitudeE6()), coordinate.getName(), "");

        this.cacheType = type;
        this.coord = coordinate;
    }

    public cgCoord getCoord() {
        return coord;
    }

    public CacheType getType() {
        return cacheType;
    }

    @Override
    public Drawable getMarker(int index) {
        return getMarker();
    }

}
