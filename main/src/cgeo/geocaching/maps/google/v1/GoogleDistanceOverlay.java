package cgeo.geocaching.maps.google.v1;

import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.DistanceOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import android.graphics.Canvas;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GoogleDistanceOverlay extends Overlay implements OverlayImpl {

    private final DistanceOverlay overlayBase;
    private final Lock lock = new ReentrantLock();

    public GoogleDistanceOverlay(final MapViewImpl mapView, final Geopoint coords, final String geocode) {
        overlayBase = new DistanceOverlay(this, mapView, coords, geocode);
    }

    @Override
    public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {
        super.draw(canvas, mapView, shadow);

        assert mapView instanceof MapViewImpl;
        overlayBase.draw(canvas, (MapViewImpl) mapView, shadow);
    }

    public GeneralOverlay getBase() {
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
        throw new UnsupportedOperationException();
    }

}
