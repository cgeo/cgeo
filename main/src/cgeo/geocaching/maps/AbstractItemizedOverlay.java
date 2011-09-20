package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.OverlayImpl;
import cgeo.geocaching.maps.interfaces.OverlayItemImpl;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

/**
 * Base class for itemized overlays. Delegates calls from deriving classes to the contained
 * provider-specific implementation.
 *
 * @author rsudev
 *
 */
public abstract class AbstractItemizedOverlay implements GeneralOverlay {

    private ItemizedOverlayImpl ovlImpl;

    protected AbstractItemizedOverlay(ItemizedOverlayImpl ovlImplIn) {
        ovlImpl = ovlImplIn;
    }

    void populate() {
        ovlImpl.superPopulate();
    }

    public boolean onTap(int index) {
        return ovlImpl.superOnTap(index);
    }

    Drawable boundCenter(Drawable markerIn) {
        return ovlImpl.superBoundCenter(markerIn);
    }

    Drawable boundCenterBottom(Drawable markerIn) {
        return ovlImpl.superBoundCenterBottom(markerIn);
    }

    void setLastFocusedItemIndex(int index) {
        ovlImpl.superSetLastFocusedItemIndex(index);
    }

    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {
        ovlImpl.superDraw(canvas, mapView, shadow);
    }

    public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {
        ovlImpl.superDrawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return ovlImpl;
    }

    public abstract OverlayItemImpl createItem(int index);

    public abstract int size();
}
