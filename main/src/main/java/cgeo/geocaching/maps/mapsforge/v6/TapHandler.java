package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;

public class TapHandler {

    private final WeakReference<NewMap> map;
    private final Set<GeoitemRef> hitItems = new HashSet<>();

    private LatLong hitRoutingPoint = null;

    private boolean longPressMode = false;

    public TapHandler(final NewMap map) {
        this.map = new WeakReference<>(map);
    }

    public synchronized void setMode(final boolean longPressMode) {
        if (this.longPressMode != longPressMode) {
            this.hitItems.clear();
            this.longPressMode = longPressMode;
        }
    }

    public synchronized void setHit(final GeoitemRef item) {
        this.hitItems.add(item);
    }

    public synchronized void setHit(final LatLong routingPoint) {
        this.hitRoutingPoint = routingPoint;
    }

    public synchronized void finished() {

        final NewMap map = this.map.get();

        // show popup
        if (map != null) {
            if (hitItems.isEmpty() && hitRoutingPoint != null) {
                map.toggleRouteItem(hitRoutingPoint);
            } else {
                map.showSelection(new ArrayList<>(hitItems), longPressMode);
            }
        }

        hitItems.clear();
        hitRoutingPoint = null;
    }

    /** returns true if longTapContextMenu got triggered */
    public boolean onLongPress(final Point tapXY) {

        final NewMap map = this.map.get();

        if (!hitItems.isEmpty() || hitRoutingPoint != null) {
            return false;
        }

        // show popup
        if (map != null) {
            map.triggerLongTapContextMenu(tapXY);
            return true;
        }
        return false;
    }
}
