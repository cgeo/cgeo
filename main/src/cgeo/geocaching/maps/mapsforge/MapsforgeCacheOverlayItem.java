package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.cgCoord;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.OverlayItem;

import android.graphics.drawable.Drawable;

public class MapsforgeCacheOverlayItem extends OverlayItem implements CachesOverlayItemImpl {
    private String cacheType = null;
    private cgCoord coord;

    public MapsforgeCacheOverlayItem(cgCoord coordinate, String type) {
        super(new GeoPoint(coordinate.getCoords().getLatitudeE6(), coordinate.getCoords().getLongitudeE6()), coordinate.getName(), "");

        this.cacheType = type;
        this.coord = coordinate;
    }

    public cgCoord getCoord() {
        return coord;
    }

    public String getType() {
        return cacheType;
    }

    @Override
    public Drawable getMarker(int index) {
        return getMarker();
    }

}
