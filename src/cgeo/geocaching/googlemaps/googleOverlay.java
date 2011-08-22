package cgeo.geocaching.googlemaps;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.graphics.Canvas;
import cgeo.geocaching.mapinterfaces.MapViewImpl;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.OverlayImpl;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class googleOverlay extends Overlay implements OverlayImpl {

    private static Lock lock = new ReentrantLock();

    public static void lock() {
        lock.lock();
    }

    public static void unlock() {
        lock.unlock();
    }

	private OverlayBase overlayBase;
	
	public googleOverlay(OverlayBase overlayBaseIn) {
		overlayBase = overlayBaseIn;
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
	    lock();
		super.draw(canvas, mapView, shadow);
		overlayBase.draw(canvas, (MapViewImpl) mapView, shadow);
		unlock();
	}

}
