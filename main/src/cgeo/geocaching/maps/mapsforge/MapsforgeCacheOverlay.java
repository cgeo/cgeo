package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.mapsforge.v3.android.maps.Projection;
import org.mapsforge.v3.android.maps.overlay.ItemizedOverlay;

public class MapsforgeCacheOverlay extends ItemizedOverlay<MapsforgeCacheOverlayItem> implements ItemizedOverlayImpl {

    @NonNull
    private final MapsforgeCachesList base;
    @NonNull
    private final Lock lock = new ReentrantLock();

    public MapsforgeCacheOverlay(final Drawable markerIn) {
        super(markerIn == null ? null : boundCenterBottom(markerIn));
        base = new MapsforgeCachesList(this);
    }

    @NonNull
    public MapsforgeCachesList getBase() {
        return base;
    }

    @Override
    protected MapsforgeCacheOverlayItem createItem(final int i) {
        return (MapsforgeCacheOverlayItem) base.createItem(i);
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
    protected void drawOverlayBitmap(final Canvas canvas, final Point drawPosition,
            final Projection projection, final byte drawZoomLevel) {
        base.drawOverlayBitmap(canvas, drawPosition, new MapsforgeMapProjection(projection), drawZoomLevel);
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
        // nothing to do
    }

    @Override
    public boolean superOnTap(final int index) {
        return super.onTap(index);
    }

    @Override
    public void superDraw(final Canvas canvas, final MapViewImpl mapView, final boolean shadow) {
        // nothing to do here...
    }

    @Override
    public void superDrawOverlayBitmap(final Canvas canvas, final Point drawPosition,
            final MapProjectionImpl projection, final byte drawZoomLevel) {
        super.drawOverlayBitmap(canvas, drawPosition, (Projection) projection.getImpl(), drawZoomLevel);
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
