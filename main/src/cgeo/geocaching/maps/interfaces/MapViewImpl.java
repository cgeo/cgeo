package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.OtherCachersOverlay;
import cgeo.geocaching.maps.PositionOverlay;
import cgeo.geocaching.maps.ScaleOverlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * Defines common functions of the provider-specific
 * MapView implementations
 *
 * @author rsudev
 *
 */
public interface MapViewImpl {

    void invalidate();

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

    CachesOverlay createAddMapOverlay(Context context,
            Drawable drawable, boolean fromDetailIntent);

    OtherCachersOverlay createAddUsersOverlay(Context context, Drawable markerIn);

    ScaleOverlay createAddScaleOverlay(Activity activity);

    PositionOverlay createAddPositionOverlay(Activity activity);

    boolean needsScaleOverlay();

    void setBuiltinScale(boolean b);

    void setMapSource();

    void repaintRequired(GeneralOverlay overlay);

    void setOnDragListener(OnDragListener onDragListener);
}
