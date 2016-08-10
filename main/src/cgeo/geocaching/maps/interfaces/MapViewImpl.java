package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;

import android.content.Context;
import android.os.Bundle;

import java.util.Collection;

/**
 * Defines common functions of the provider-specific
 * MapView implementations
 */
public interface MapViewImpl<T extends CachesOverlayItemImpl> {

    void setBuiltInZoomControls(boolean b);

    void displayZoomControls(boolean b);

    void clearOverlays();

    MapControllerImpl getMapController();

    void destroyDrawingCache();

    GeoPointImpl getMapViewCenter();

    int getLatitudeSpan();

    int getLongitudeSpan();

    int getMapZoomLevel();

    float getBearing();

    int getWidth();

    int getHeight();

    MapProjectionImpl getMapProjection();

    Context getContext();

    PositionAndHistory createAddPositionAndScaleOverlay(Geopoint coords);

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

    void onMapReady(MapReadyCallback callback);

    void updateItems(Collection<T> itemsPre);

    boolean getCircles();

    void switchCircles();

    void setOnTapListener(OnCacheTapListener listener);


    /* From Google MapView documentation:
     * Users of this class must forward all the life cycle methods from the Activity or Fragment
     * containing this view to the corresponding ones in this class. In particular, you must
     * forward on the following methods:
     */
    void onCreate(Bundle b);
    void onResume();
    void onPause();
    void onDestroy();
    void onSaveInstanceState(Bundle b);
    void onLowMemory();
}
