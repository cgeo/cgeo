package cgeo.geocaching.maps.google.v2;

import android.os.Handler;
import android.os.Looper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;

public class GoogleMapObjectsQueue {


    private final GoogleMap googleMap;

    private boolean repaintRequested = false;

    private final ConcurrentLinkedQueue<MapObjectOptions> requestedToAdd = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MapObjectOptions> requestedToRemove = new ConcurrentLinkedQueue<>();

    private final RepaintRunner repaintRunner = new RepaintRunner();

    private final Lock lock = new ReentrantLock();


    public GoogleMapObjectsQueue(final GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    public void requestAdd(final Collection<? extends MapObjectOptions> toAdd) {
        requestedToAdd.addAll(toAdd);
        requestRepaint();
    }

    public void requestAdd(final MapObjectOptions toAdd) {
        requestedToAdd.add(toAdd);
        requestRepaint();
    }

    public void requestRemove(final Collection<? extends MapObjectOptions> toRemove) {
        requestedToRemove.addAll(toRemove);
        requestRepaint();
    }

    void requestRepaint() {
        lock.lock();
        if (!repaintRequested) {
            repaintRequested = true;
            runOnUIThread(repaintRunner);
        }
        lock.unlock();
    }

    public void runOnUIThread(final Runnable runnable) {
        // inspired by http://stackoverflow.com/questions/12850143/android-basics-running-code-in-the-ui-thread/25250494#25250494
        // modifications of google map must be run on main (UI) thread
        new Handler(Looper.getMainLooper()).post(runnable);
    }


    private static void removeDrawnObject(final Object obj) {
        if (obj == null) {
            return; // failsafe
        }
        if (obj instanceof Marker) {
            ((Marker) obj).remove();
        } else if (obj instanceof Circle) {
            ((Circle) obj).remove();
        } else if (obj instanceof Polyline) {
            ((Polyline) obj).remove();
        } else if (obj instanceof Polygon) {
            ((Polygon) obj).remove();
        } else {
            throw new IllegalStateException();
        }
    }

    private class RepaintRunner implements Runnable {

        /**
         * magic number of milliseconds. maximum allowed time of adding or removing items to googlemap
         */
        protected static final long TIME_MAX = 40;

        private final Map<MapObjectOptions, Object> drawObjects = new HashMap<>();

        private boolean removeRequested() {
            final long time = System.currentTimeMillis();
            MapObjectOptions options;
            while ((options = requestedToRemove.poll()) != null) {
                final Object obj = drawObjects.get(options);
                if (obj != null) {
                    removeDrawnObject(obj);
                    drawObjects.remove(options);
                } else {
                    // could not remove, is it enqueued to be draw?
                    if (requestedToAdd.contains(options)) {
                        // if yes, it is not anymore
                        requestedToAdd.remove(options);
                    }
                }
                if (System.currentTimeMillis() - time >= TIME_MAX) {
                    // removing and adding markers are time costly operations and we don't want to block UI thread
                    runOnUIThread(this);
                    return false;
                }
            }
            return true;
        }

        @Override
        public void run() {
            lock.lock();
            if (repaintRequested && removeRequested() && addRequested()) {
                // repaint successful, set flag to false
                repaintRequested = false;
            }
            lock.unlock();
        }

        private boolean addRequested() {
            final long time = System.currentTimeMillis();
            MapObjectOptions options;
            while ((options = requestedToAdd.poll()) != null) {
                // avoid redrawing exactly the same accuracy circle, as sometimes consecutive identical circles remain on the map
                if (!(options.options instanceof CircleOptions) || ((CircleOptions) options.options).getZIndex() != GooglePositionAndHistory.ZINDEX_POSITION_ACCURACY_CIRCLE || !drawObjects.containsKey(options)) {
                    final Object drawn = options.addToGoogleMap(googleMap);
                    drawObjects.put(options, drawn);
                }
                if (System.currentTimeMillis() - time >= TIME_MAX) {
                    // removing and adding markers are time costly operations and we dont want to block UI thread
                    runOnUIThread(this);
                    return false;
                }
            }
            return true;
        }

    }
}
