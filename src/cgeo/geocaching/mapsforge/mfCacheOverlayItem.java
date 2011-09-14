package cgeo.geocaching.mapsforge;

import cgeo.geocaching.cgCoord;
import cgeo.geocaching.mapinterfaces.CacheOverlayItemImpl;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.OverlayItem;

import android.graphics.drawable.Drawable;

public class mfCacheOverlayItem extends OverlayItem implements CacheOverlayItemImpl {
    private String cacheType = null;
    private cgCoord coord;

    public mfCacheOverlayItem(cgCoord coordinate, String type) {
        super(new GeoPoint(coordinate.coords.getLatitudeE6(), coordinate.coords.getLongitudeE6()), coordinate.name, "");

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
