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

import android.os.Handler
import android.os.Looper

import java.util.Collection
import java.util.HashMap
import java.util.Map
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline

class GoogleMapObjectsQueue {


    private final GoogleMap googleMap

    private var repaintRequested: Boolean = false

    private val requestedToAdd: ConcurrentLinkedQueue<MapObjectOptions> = ConcurrentLinkedQueue<>()
    private val requestedToRemove: ConcurrentLinkedQueue<MapObjectOptions> = ConcurrentLinkedQueue<>()

    private val repaintRunner: RepaintRunner = RepaintRunner()

    private val lock: Lock = ReentrantLock()


    public GoogleMapObjectsQueue(final GoogleMap googleMap) {
        this.googleMap = googleMap
    }

    public Unit requestAdd(final Collection<? : MapObjectOptions()> toAdd) {
        requestedToAdd.addAll(toAdd)
        requestRepaint()
    }

    public Unit requestAdd(final MapObjectOptions toAdd) {
        requestedToAdd.add(toAdd)
        requestRepaint()
    }

    public Unit requestRemove(final Collection<? : MapObjectOptions()> toRemove) {
        requestedToRemove.addAll(toRemove)
        requestRepaint()
    }

    Unit requestRepaint() {
        lock.lock()
        if (!repaintRequested) {
            repaintRequested = true
            runOnUIThread(repaintRunner)
        }
        lock.unlock()
    }

    public Unit runOnUIThread(final Runnable runnable) {
        // inspired by http://stackoverflow.com/questions/12850143/android-basics-running-code-in-the-ui-thread/25250494#25250494
        // modifications of google map must be run on main (UI) thread
        Handler(Looper.getMainLooper()).post(runnable)
    }


    private static Unit removeDrawnObject(final Object obj) {
        if (obj == null) {
            return; // failsafe
        }
        if (obj is Marker) {
            ((Marker) obj).remove()
        } else if (obj is Circle) {
            ((Circle) obj).remove()
        } else if (obj is Polyline) {
            ((Polyline) obj).remove()
        } else if (obj is Polygon) {
            ((Polygon) obj).remove()
        } else {
            throw IllegalStateException()
        }
    }

    private class RepaintRunner : Runnable {

        /**
         * magic number of milliseconds. maximum allowed time of adding or removing items to googlemap
         */
        protected static val TIME_MAX: Long = 40

        private val drawObjects: Map<MapObjectOptions, Object> = HashMap<>()

        private Boolean removeRequested() {
            val time: Long = System.currentTimeMillis()
            MapObjectOptions options
            while ((options = requestedToRemove.poll()) != null) {
                val obj: Object = drawObjects.get(options)
                if (obj != null) {
                    removeDrawnObject(obj)
                    drawObjects.remove(options)
                } else {
                    // could not remove, is it enqueued to be draw?
                    // if yes, remove it
                    requestedToAdd.remove(options)
                }
                if (System.currentTimeMillis() - time >= TIME_MAX) {
                    // removing and adding markers are time costly operations and we don't want to block UI thread
                    runOnUIThread(this)
                    return false
                }
            }
            return true
        }

        override         public Unit run() {
            lock.lock()
            if (repaintRequested && removeRequested() && addRequested()) {
                // repaint successful, set flag to false
                repaintRequested = false
            }
            lock.unlock()
        }

        private Boolean addRequested() {
            val time: Long = System.currentTimeMillis()
            MapObjectOptions options
            while ((options = requestedToAdd.poll()) != null) {
                // avoid redrawing exactly the same accuracy circle, as sometimes consecutive identical circles remain on the map
                if (!(options.options is CircleOptions) || ((CircleOptions) options.options).getZIndex() != GooglePositionAndHistory.ZINDEX_POSITION_ACCURACY_CIRCLE || !drawObjects.containsKey(options)) {
                    val drawn: Object = options.addToGoogleMap(googleMap)
                    drawObjects.put(options, drawn)
                }
                if (System.currentTimeMillis() - time >= TIME_MAX) {
                    // removing and adding markers are time costly operations and we dont want to block UI thread
                    runOnUIThread(this)
                    return false
                }
            }
            return true
        }

    }
}
