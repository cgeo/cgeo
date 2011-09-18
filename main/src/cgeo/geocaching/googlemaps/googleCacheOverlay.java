package cgeo.geocaching.googlemaps;

import cgeo.geocaching.cgSettings;
import cgeo.geocaching.mapcommon.cgMapOverlay;
import cgeo.geocaching.mapinterfaces.ItemizedOverlayImpl;
import cgeo.geocaching.mapinterfaces.MapProjectionImpl;
import cgeo.geocaching.mapinterfaces.MapViewImpl;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Google specific implementation of the itemized cache overlay
 *
 * @author rsudev
 *
 */
public class googleCacheOverlay extends ItemizedOverlay<googleCacheOverlayItem> implements ItemizedOverlayImpl {

    private cgMapOverlay base;
    private Lock lock = new ReentrantLock();

    public googleCacheOverlay(cgSettings settingsIn, Context contextIn, Drawable markerIn, Boolean fromDetailIn) {
        super(boundCenterBottom(markerIn));
        base = new cgMapOverlay(settingsIn, this, contextIn, fromDetailIn);
    }

    @Override
    public cgMapOverlay getBase() {
        return base;
    }

    @Override
    protected googleCacheOverlayItem createItem(int i) {
        if (base == null)
            return null;

        return (googleCacheOverlayItem) base.createItem(i);
    }

    @Override
    public int size() {
        if (base == null)
            return 0;

        return base.size();
    }

    @Override
    protected boolean onTap(int arg0) {
        if (base == null)
            return false;

        return base.onTap(arg0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event, MapView mapView) {

        boolean result = false;
        // prevent concurrent access
        lock();
        try {
            result = super.onTouchEvent(event, mapView);
        } catch (Exception e) {
            Log.e(cgSettings.tag, "Exception during onTouchEvent", e);
        } finally {
            unlock();
        }

        return result;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (base != null) {
            base.draw(canvas, (MapViewImpl) mapView, shadow);
        }
    }

    @Override
    public void superPopulate() {
        populate();
    }

    @Override
    public Drawable superBoundCenter(Drawable markerIn) {
        return super.boundCenter(markerIn);
    }

    @Override
    public Drawable superBoundCenterBottom(Drawable marker) {
        return super.boundCenterBottom(marker);
    }

    @Override
    public void superSetLastFocusedItemIndex(int i) {
        super.setLastFocusedIndex(i);
    }

    @Override
    public boolean superOnTap(int index) {
        return super.onTap(index);
    }

    @Override
    public void superDraw(Canvas canvas, MapViewImpl mapView, boolean shadow) {
        super.draw(canvas, (MapView) mapView, shadow);
    }

    @Override
    public void superDrawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {
        // Nothing to do here...
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
