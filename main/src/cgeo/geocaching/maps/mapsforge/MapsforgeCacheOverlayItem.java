package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;

import org.mapsforge.v3.android.maps.overlay.OverlayItem;
import org.mapsforge.v3.core.GeoPoint;

import android.graphics.drawable.Drawable;

public class MapsforgeCacheOverlayItem extends OverlayItem implements CachesOverlayItemImpl {
    private final IWaypoint coord;
    private final boolean applyDistanceRule;

    public MapsforgeCacheOverlayItem(final IWaypoint coordinate, final boolean applyDistanceRule) {
        super(new GeoPoint(coordinate.getCoords().getLatitudeE6(), coordinate.getCoords().getLongitudeE6()), coordinate.getName(), "");

        this.coord = coordinate;
        this.applyDistanceRule = applyDistanceRule;
    }

    @Override
    public IWaypoint getCoord() {
        return coord;
    }

    @Override
    public Drawable getMarker(final int index) {
        return getMarker();
    }

    @Override
    public boolean applyDistanceRule() {
        return applyDistanceRule;
    }

}
