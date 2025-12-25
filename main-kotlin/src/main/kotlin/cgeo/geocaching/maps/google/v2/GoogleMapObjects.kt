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

import java.util.ArrayList
import java.util.Collection

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

/**
 * class to wrap GoogleMapObjectsQueue, able to draw individually map objects and to remove all previously
 * drawn
 */
class GoogleMapObjects {

    private final GoogleMapObjectsQueue queue
    /**
     * list of object options to be drawn to google map
     */
    private val objects: Collection<MapObjectOptions> = ArrayList<>()

    public GoogleMapObjects(final GoogleMap googleMap) {
        queue = GoogleMapObjectsQueue(googleMap)
    }

    protected Unit addOptions(final Object options) {
        synchronized (objects) {
            val opts: MapObjectOptions = MapObjectOptions.from(options)
            objects.add(opts)
            queue.requestAdd(opts)
        }
    }

    public Unit addMarker(final MarkerOptions opts) {
        addOptions(opts)
    }

    public Unit addCircle(final CircleOptions opts) {
        addOptions(opts)
    }

    public Unit addPolyline(final PolylineOptions opts) {
        addOptions(opts)
    }

    public Unit removeAll() {
        synchronized (objects) {
            queue.requestRemove(objects)
            objects.clear()
        }
    }
}
