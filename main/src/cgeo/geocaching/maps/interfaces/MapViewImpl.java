package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.PositionOverlay;
import cgeo.geocaching.maps.ScaleOverlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

/**
 * Defines common functions of the provider-specific
 * MapView implementations
 */
public interface MapViewImpl {

    void setBuiltInZoomControls(boolean b);

    void displayZoomControls(boolean b);

    void preLoad();

    void clearOverlays();

    void addOverlay(OverlayImpl ovl);

    MapControllerImpl getMapController();

    void destroyDrawingCache();

    GeoPointImpl getMapViewCenter();

    int getLatitudeSpan();

    int getLongitudeSpan();

    int getMapZoomLevel();

    int getWidth();

    int getHeight();

    MapProjectionImpl getMapProjection();

    Context getContext();

    CachesOverlay createAddMapOverlay(Context context, Drawable drawable);

    ScaleOverlay createAddScaleOverlay(Activity activity);

    PositionOverlay createAddPositionOverlay(Activity activity);

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
     * Indicates if the view supports setting the map source that could be a map database for offline
     * *
     * 
     * @return true - switching the source is available
     */
    boolean isMapDatabaseSwitchSupported();

    /**
     * Returns a list of available sources for display to the end-user in the UI
     * Note: if an internal representation of the source such as an ID is required
     * it is the responsibility of this implemented class to hold a list mapping
     * the returned list to the ID
     * 
     * @return List of selectable sources
     */
    ArrayList<String> getMapDatabaseList();

    /**
     * Returns the current source as a name in the same styles a for the entries
     * in getMapDatabaseList to be used to display the current selection for the end user
     * 
     * @return The currently used map source og null is no source is selected
     */
    String getCurrentMapDatabase();

    /**
     * Set a the map source
     * in getMapDatabaseList to be used to display the current selection for the end user
     * 
     * @param mapSourceName
     *            The name of the source as listed in the list from getMapDatabaseList
     */
    void setMapDatabase(String mapSourceName);

}
