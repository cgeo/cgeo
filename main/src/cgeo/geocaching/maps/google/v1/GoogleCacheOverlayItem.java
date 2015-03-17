package cgeo.geocaching.maps.google.v1;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class GoogleCacheOverlayItem extends OverlayItem implements CachesOverlayItemImpl {
    final private IWaypoint coord;
    final private boolean applyDistanceRule;

    public GoogleCacheOverlayItem(final IWaypoint coordinate, final boolean applyDistanceRule) {
        super(new GeoPoint(coordinate.getCoords().getLatitudeE6(), coordinate.getCoords().getLongitudeE6()), coordinate.getName(), "");

        this.coord = coordinate;
        this.applyDistanceRule = applyDistanceRule;
    }

    @Override
    public IWaypoint getCoord() {
        return coord;
    }

    @Override
    public boolean applyDistanceRule() {
        return applyDistanceRule;
    }

}
