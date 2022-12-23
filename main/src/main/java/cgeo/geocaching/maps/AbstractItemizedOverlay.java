package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;
import cgeo.geocaching.maps.interfaces.OverlayItemImpl;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

/**
 * Base class for itemized overlays. Delegates calls from deriving classes to the contained
 * provider-specific implementation.
 */
public abstract class AbstractItemizedOverlay implements GeneralOverlay {

    private final ItemizedOverlayImpl ovlImpl;

    protected AbstractItemizedOverlay(final ItemizedOverlayImpl ovlImplIn) {
        ovlImpl = ovlImplIn;
    }

    protected void populate() {
        ovlImpl.superPopulate();
    }

    public boolean onTap(final int index) {
        return ovlImpl.superOnTap(index);
    }

    protected Drawable boundCenterBottom(final Drawable markerIn) {
        return ovlImpl.superBoundCenterBottom(markerIn);
    }

    protected void setLastFocusedItemIndex(final int index) {
        ovlImpl.superSetLastFocusedItemIndex(index);
    }

    @Override
    public void draw(final Canvas canvas, final MapViewImpl mapView, final boolean shadow) {
        ovlImpl.superDraw(canvas, mapView, shadow);
    }

    @Override
    public void drawOverlayBitmap(final Canvas canvas, final Point drawPosition,
                                  final MapProjectionImpl projection, final byte drawZoomLevel) {
        ovlImpl.superDrawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return ovlImpl;
    }

    public abstract OverlayItemImpl createItem(int index);

    public abstract int size();
}
