package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.models.IWaypoint;

import org.mapsforge.v3.android.maps.overlay.OverlayItem;
import org.mapsforge.v3.core.GeoPoint;

public class MapsforgeCacheOverlayItem extends OverlayItem implements CachesOverlayItemImpl {
    private final IWaypoint coord;
    private final boolean applyDistanceRule;

    private CacheMarker marker;

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
    public CacheMarker getMarker(final int index) {
        return marker;
    }

    public void setMarker(final CacheMarker marker) {
        this.marker = marker;
        setMarker(marker.getDrawable());
    }

    @Override
    public boolean applyDistanceRule() {
        return applyDistanceRule;
    }

}
