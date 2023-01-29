package cgeo.geocaching.maps.google.v2;

import com.google.android.gms.maps.GoogleMap;

/**
 * class to wrap GoogleMapObjectsQueue
 */
public class GoogleMapObjects {

    private final GoogleMapObjectsQueue queue;

    public GoogleMapObjects(final GoogleMap googleMap) {
        queue = new GoogleMapObjectsQueue(googleMap);
    }

    public void add(final MapObjectOptions opt) {
        queue.add(opt);
    }

    public void removeAll() {
        queue.removeAll();
    }
}
