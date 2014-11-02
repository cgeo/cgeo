package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.PositionAndScaleOverlay;

import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * Defines common functions of the provider-specific
 * MapView implementations
 */
public interface MapViewImpl {

    void setBuiltInZoomControls(boolean b);

    void displayZoomControls(boolean b);

    void preLoad();

    void clearOverlays();

    MapControllerImpl getMapController();

    void destroyDrawingCache();

    @NonNull
    GeoPointImpl getMapViewCenter();

    int getLatitudeSpan();

    int getLongitudeSpan();

    int getMapZoomLevel();

    int getWidth();

    int getHeight();

    MapProjectionImpl getMapProjection();

    Context getContext();

    CachesOverlay createAddMapOverlay(Context context, Drawable drawable);

    PositionAndScaleOverlay createAddPositionAndScaleOverlay();

    void setMapSource();

    /**
     * Map-library unspecific method to request a repaint of either
     * a specific overlay, that changed, or the mapview as a whole
     * (if overlay is null)
     *
     * @param overlay
     *            Overlay to repaint or null if the mapview has changed
     */
    void repaintRequired(GeneralOverlay overlay);

    void setOnDragListener(OnMapDragListener onDragListener);

    /**
     * Indicates if overlay text or line colours should be dark (normal case)
     * or light (inverted case)
     *
     * @return true - text/draw in light colors, false text/draw in dark colors
     */
    boolean needsInvertedColors();

    Viewport getViewport();

    /**
     * Indicates if the current map view supports different themes
     * for map rendering
     *
     * @return true - supports custom themes, false - does not support custom themes
     */
    boolean hasMapThemes();

    void setMapTheme();
}
