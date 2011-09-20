package cgeo.geocaching.maps.google;

import cgeo.geocaching.cgSettings;
import cgeo.geocaching.maps.PositionOverlay;
import cgeo.geocaching.maps.ScaleOverlay;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import android.app.Activity;
import android.graphics.Canvas;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GoogleOverlay extends Overlay implements OverlayImpl {

    private GeneralOverlay overlayBase = null;
    private Lock lock = new ReentrantLock();

    public GoogleOverlay(Activity activityIn, cgSettings settingsIn, overlayType ovlType) {
        switch (ovlType) {
            case PositionOverlay:
                overlayBase = new PositionOverlay(settingsIn, activityIn, this);
                break;
            case ScaleOverlay:
                overlayBase = new ScaleOverlay(activityIn, settingsIn, this);
        }
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);

        if (overlayBase != null) {
            overlayBase.draw(canvas, (MapViewImpl) mapView, shadow);
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
}
