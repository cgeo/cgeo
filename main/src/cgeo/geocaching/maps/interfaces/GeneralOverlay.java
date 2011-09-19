package cgeo.geocaching.maps.interfaces;

import android.graphics.Canvas;
import android.graphics.Point;

/**
 * Defines the base functions of the provider-independent
 * Overlay implementations
 *
 * @author rsudev
 *
 */
public interface GeneralOverlay {

    void draw(Canvas canvas, MapViewImpl mapView, boolean shadow);

    void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel);

    OverlayImpl getOverlayImpl();
}
