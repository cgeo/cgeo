package cgeo.geocaching.maps.mapsforge.v024;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.DirectionOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import org.mapsforge.android.mapsold.Overlay;
import org.mapsforge.android.mapsold.Projection;

import android.graphics.Canvas;
import android.graphics.Point;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapsforgeDirectionOverlay extends Overlay implements OverlayImpl {

    private DirectionOverlay overlayBase = null;
    private final Lock lock = new ReentrantLock();

    public MapsforgeDirectionOverlay(final MapViewImpl mapView, final Geopoint coords, final String geocode) {
        overlayBase = new DirectionOverlay(this, mapView, coords, geocode);
    }

    @Override
    protected void drawOverlayBitmap(final Canvas canvas, final Point drawPosition,
            final Projection projection, final byte drawZoomLevel) {

        if (overlayBase != null) {
            overlayBase.drawOverlayBitmap(canvas, drawPosition, new MapsforgeMapProjection(projection), drawZoomLevel);
        }
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
        return (MapViewImpl) internalMapView;
    }

}
