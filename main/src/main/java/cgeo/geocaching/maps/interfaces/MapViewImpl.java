package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    void zoomToBounds(Viewport bounds, GeoPointImpl center);

    float getBearing();

    int getWidth();

    int getHeight();

    void setDestinationCoords(Geopoint destCoords);

    void setCoordinates(Location coordinates);

    MapProjectionImpl getMapProjection();

    Context getContext();

    PositionAndHistory createAddPositionAndScaleOverlay(View root, Geopoint coords, String geocode);

    void setMapSource();

    /**
     * Map-library unspecific method to request a repaint of either
     * a specific overlay, that changed, or the mapview as a whole
     * (if overlay is null)
     *
     * @param overlay Overlay to repaint or null if the mapview has changed
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

    void onMapReady(MapReadyCallback callback);

    void updateItems(Collection<T> itemsPre);

    boolean getCircles();

    void setCircles(boolean showCircles);

    void setOnTapListener(OnCacheTapListener listener);

    void setListId(int listId);

    /* From Google MapView documentation:
     * Users of this class must forward all the life cycle methods from the Activity or Fragment
     * containing this view to the corresponding ones in this class. In particular, you must
     * forward on the following methods:
     */
    void onCreate(Bundle b);

    void onResume();

    void onPause();

    void onDestroy();

    void onSaveInstanceState(@NonNull Bundle b);

    void onLowMemory();

    void selectMapTheme(AppCompatActivity activity);
}
