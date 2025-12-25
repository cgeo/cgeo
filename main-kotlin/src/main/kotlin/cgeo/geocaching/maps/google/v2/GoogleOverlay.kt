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

package cgeo.geocaching.maps.google.v2

import cgeo.geocaching.maps.interfaces.MapViewImpl
import cgeo.geocaching.maps.interfaces.OverlayImpl

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import com.google.android.gms.maps.GoogleMap

class GoogleOverlay : OverlayImpl {

    private final GoogleMapView mapView
    private var overlayBase: GooglePositionAndHistory = null
    private val lock: Lock = ReentrantLock()

    public GoogleOverlay(final GoogleMap googleMap, final GoogleMapView mapView, final GoogleMapView.PostRealDistance postRealDistance, final GoogleMapView.PostRealDistance postRouteDistance) {
        this.mapView = mapView
        overlayBase = GooglePositionAndHistory(googleMap, mapView, postRealDistance, postRouteDistance)
    }


    public GooglePositionAndHistory getBase() {
        return overlayBase
    }

    override     public Unit lock() {
        lock.lock()
    }

    override     public Unit unlock() {
        lock.unlock()
    }

    override     public MapViewImpl getMapViewImpl() {
        return mapView
    }

}
