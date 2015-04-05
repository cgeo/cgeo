package cgeo.geocaching.maps.google.v1;

import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;

import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Google specific implementation of the itemized cache overlay
 */
public class GoogleCacheOverlay extends ItemizedOverlay<GoogleCacheOverlayItem> implements ItemizedOverlayImpl {

    @NonNull
    private final CachesOverlay base;
    @NonNull
    private final Lock lock = new ReentrantLock();

    public GoogleCacheOverlay(final Context contextIn, final Drawable markerIn) {
        super(boundCenterBottom(markerIn));
        base = new CachesOverlay(this, contextIn);
    }

    @Override
    public CachesOverlay getBase() {
        return base;
    }

    @Override
    protected GoogleCacheOverlayItem createItem(final int i) {
        return (GoogleCacheOverlayItem) base.createItem(i);
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    protected boolean onTap(final int arg0) {
        return base.onTap(arg0);
    }

    @Override
    public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {
        base.draw(canvas, castMapViewImpl(mapView), shadow);
    }

    private static MapViewImpl castMapViewImpl(final MapView mapView) {
        assert mapView instanceof MapViewImpl;
        return (MapViewImpl) mapView;
    }

    @Override
    public void superPopulate() {
        populate();
    }

    @Override
    public Drawable superBoundCenterBottom(final Drawable marker) {
        return ItemizedOverlay.boundCenterBottom(marker);
    }

    @Override
    public void superSetLastFocusedItemIndex(final int i) {
        super.setLastFocusedIndex(i);
    }

    @Override
    public boolean superOnTap(final int index) {
        return super.onTap(index);
    }

    @Override
    public void superDraw(final Canvas canvas, final MapViewImpl mapView, final boolean shadow) {
        super.draw(canvas, (MapView) mapView, shadow);
    }

    @Override
    public void superDrawOverlayBitmap(final Canvas canvas, final Point drawPosition,
            final MapProjectionImpl projection, final byte drawZoomLevel) {
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

    @Override
    public MapViewImpl getMapViewImpl() {
        throw new UnsupportedOperationException();
    }

}
