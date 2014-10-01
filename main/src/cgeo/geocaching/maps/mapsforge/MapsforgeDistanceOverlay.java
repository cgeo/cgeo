package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.DistanceOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.Overlay;

import android.graphics.Canvas;
import android.graphics.Point;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapsforgeDistanceOverlay extends Overlay implements OverlayImpl {

    private DistanceOverlay overlayBase = null;
    private final Lock lock = new ReentrantLock();

    public MapsforgeDistanceOverlay(final Geopoint coords, final String geocode) {
        overlayBase = new DistanceOverlay(this, coords, geocode);
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
