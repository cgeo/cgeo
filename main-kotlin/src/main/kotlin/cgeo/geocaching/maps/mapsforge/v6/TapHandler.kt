// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps.mapsforge.v6

import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.HashSet
import java.util.Set

import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point

class TapHandler {

    private final WeakReference<NewMap> map
    private val hitItems: Set<GeoitemRef> = HashSet<>()

    private var hitRoutingPoint: LatLong = null

    private var longPressMode: Boolean = false

    public TapHandler(final NewMap map) {
        this.map = WeakReference<>(map)
    }

    public synchronized Unit setMode(final Boolean longPressMode) {
        if (this.longPressMode != longPressMode) {
            this.hitItems.clear()
            this.longPressMode = longPressMode
        }
    }

    public synchronized Unit setHit(final GeoitemRef item) {
        this.hitItems.add(item)
    }

    public synchronized Unit setHit(final LatLong routingPoint) {
        this.hitRoutingPoint = routingPoint
    }

    public synchronized Unit finished() {

        val map: NewMap = this.map.get()

        // show popup
        if (map != null) {
            if (hitItems.isEmpty() && hitRoutingPoint != null) {
                map.toggleRouteItem(hitRoutingPoint)
            } else {
                map.showSelection(ArrayList<>(hitItems), longPressMode)
            }
        }

        hitItems.clear()
        hitRoutingPoint = null
    }

    /** returns true if longTapContextMenu got triggered */
    public Boolean onLongPress(final Point tapXY) {

        val map: NewMap = this.map.get()

        if (!hitItems.isEmpty() || hitRoutingPoint != null) {
            return false
        }

        // show popup
        if (map != null) {
            map.triggerLongTapContextMenu(tapXY)
            return true
        }
        return false
    }
}
