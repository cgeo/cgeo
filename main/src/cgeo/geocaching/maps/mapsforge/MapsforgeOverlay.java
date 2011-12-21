package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.PositionOverlay;
import cgeo.geocaching.maps.ScaleOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import org.mapsforge.android.maps.Overlay;
import org.mapsforge.android.maps.Projection;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Point;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapsforgeOverlay extends Overlay implements OverlayImpl {

    private GeneralOverlay overlayBase = null;
    private Lock lock = new ReentrantLock();

    public MapsforgeOverlay(Activity activityIn, OverlayImpl.overlayType ovlType) {

        switch (ovlType) {
            case PositionOverlay:
                overlayBase = new PositionOverlay(activityIn, this);
                break;
            case ScaleOverlay:
                overlayBase = new ScaleOverlay(activityIn, this);
        }
    }

    @Override
    protected void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            Projection projection, byte drawZoomLevel) {

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
