package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.OtherCachersOverlay;
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

    OtherCachersOverlay createAddUsersOverlay(Context context, Drawable markerIn);

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

    boolean isMapDatabaseSwitchSupported();

    ArrayList<String> getMapDatabaseList();

    String getCurrentMapDatabase();

    void setMapDatabase(String s);

}
