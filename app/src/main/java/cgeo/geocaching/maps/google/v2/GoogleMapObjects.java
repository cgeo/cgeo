package cgeo.geocaching.maps.google.v2;

import java.util.ArrayList;
import java.util.Collection;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * class to wrap GoogleMapObjectsQueue, able to draw individually map objects and to remove all previously
 * drawn
 */
public class GoogleMapObjects {

    private final GoogleMapObjectsQueue queue;
    /**
     * list of object options to be drawn to google map
     */
    private final Collection<MapObjectOptions> objects = new ArrayList<>();

    public GoogleMapObjects(final GoogleMap googleMap) {
        queue = new GoogleMapObjectsQueue(googleMap);
    }

    protected void addOptions(final Object options) {
        synchronized (objects) {
            final MapObjectOptions opts = MapObjectOptions.from(options);
            objects.add(opts);
            queue.requestAdd(opts);
        }
    }

    public void addMarker(final MarkerOptions opts) {
        addOptions(opts);
    }

    public void addCircle(final CircleOptions opts) {
        addOptions(opts);
    }

    public void addPolyline(final PolylineOptions opts) {
        addOptions(opts);
    }


    public void removeAll() {
        synchronized (objects) {
            queue.requestRemove(objects);
            objects.clear();
        }
    }
}
