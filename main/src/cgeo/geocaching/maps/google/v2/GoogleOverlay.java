package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.android.gms.maps.GoogleMap;

public class GoogleOverlay implements OverlayImpl {

    private final GoogleMapView mapView;
    private GooglePositionAndHistory overlayBase = null;
    private final Lock lock = new ReentrantLock();

    public GoogleOverlay(final GoogleMap googleMap, final GoogleMapView mapView) {
        this.mapView = mapView;
        overlayBase = new GooglePositionAndHistory(googleMap, mapView);
    }


    public GooglePositionAndHistory getBase() {
        return overlayBase;
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public MapViewImpl getMapViewImpl() {
        return mapView;
    }

}
