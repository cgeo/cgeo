package cgeo.geocaching.maps.mapsforge.v024;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.DistanceOverlay;
import cgeo.geocaching.maps.PositionAndScaleOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import org.eclipse.jdt.annotation.NonNull;
import org.mapsforge.android.mapsold.GeoPoint;
import org.mapsforge.android.mapsold.MapDatabase;
import org.mapsforge.android.mapsold.MapView;
import org.mapsforge.android.mapsold.MapViewMode;
import org.mapsforge.android.mapsold.Overlay;
import org.mapsforge.android.mapsold.Projection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.Toast;
public class MapsforgeMapView024 extends MapView implements MapViewImpl {
    private GestureDetector gestureDetector;
    private OnMapDragListener onDragListener;
    private final MapsforgeMapController mapController = new MapsforgeMapController(getController(), getMaxZoomLevel());

    public MapsforgeMapView024(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(final Context context) {
        if (isInEditMode()) {
            return;
        }
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        try {
            // Google Maps and OSM Maps use different zoom levels for the same view.
            // Here we don't want the Google Maps compatible zoom level, but the actual one.
            if (getActualMapZoomLevel() > 22) { // to avoid too close zoom level (mostly on Samsung Galaxy S series)
                getController().setZoom(22);
            }

            super.draw(canvas);
        } catch (final Exception e) {
            Log.e("MapsforgeMapView024.draw", e);
        }
    }

    @Override
    public void displayZoomControls(final boolean takeFocus) {
        // nothing to do here
    }

    @Override
    public MapControllerImpl getMapController() {
        return mapController;
    }

    @Override
    @NonNull
    public GeoPointImpl getMapViewCenter() {
        final GeoPoint point = getMapCenter();
        return new MapsforgeGeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
    }

    @Override
    public Viewport getViewport() {
        return new Viewport(getMapViewCenter(), getLatitudeSpan() / 1e6, getLongitudeSpan() / 1e6);
    }

    @Override
    public void clearOverlays() {
        getOverlays().clear();
    }

    @Override
    public MapProjectionImpl getMapProjection() {
        return new MapsforgeMapProjection(getProjection());
    }

    @Override
    public CachesOverlay createAddMapOverlay(final Context context, final Drawable drawable) {

        final MapsforgeCacheOverlay ovl = new MapsforgeCacheOverlay(context, drawable);
        getOverlays().add(ovl);
        return ovl.getBase();
    }

    @Override
    public PositionAndScaleOverlay createAddPositionAndScaleOverlay() {
        final MapsforgeOverlay ovl = new MapsforgeOverlay();
        getOverlays().add(ovl);
        return (PositionAndScaleOverlay) ovl.getBase();
    }

    @Override
    public DistanceOverlay createAddDistanceOverlay(final Geopoint coords, final String geocode) {
        final MapsforgeDistanceOverlay ovl = new MapsforgeDistanceOverlay(this, coords, geocode);
        getOverlays().add(ovl);
        return (DistanceOverlay) ovl.getBase();
    }

    @Override
    public int getLatitudeSpan() {

        int span = 0;

        final Projection projection = getProjection();

        if (projection != null && getHeight() > 0) {

            final GeoPoint low = projection.fromPixels(0, 0);
            final GeoPoint high = projection.fromPixels(0, getHeight());

            if (low != null && high != null) {
                span = Math.abs(high.getLatitudeE6() - low.getLatitudeE6());
            }
        }

        return span;
    }

    @Override
    public int getLongitudeSpan() {

        int span = 0;

        final Projection projection = getProjection();

        if (projection != null && getWidth() > 0) {
            final GeoPoint low = projection.fromPixels(0, 0);
            final GeoPoint high = projection.fromPixels(getWidth(), 0);

            if (low != null && high != null) {
                span = Math.abs(high.getLongitudeE6() - low.getLongitudeE6());
            }
        }

        return span;
    }

    @Override
    public void preLoad() {
        // Nothing to do here
    }

    /**
     * Get the map zoom level which is compatible with Google Maps.
     *
     * @return the current map zoom level +1
     */
    @Override
    public int getMapZoomLevel() {
        // Google Maps and OSM Maps use different zoom levels for the same view.
        // All OSM Maps zoom levels are offset by 1 so they match Google Maps.
        return getZoomLevel() + 1;
    }

    /**
     * Get the actual map zoom level
     *
     * @return the current map zoom level with no adjustments
     */
    private int getActualMapZoomLevel() {
        return getZoomLevel();
    }

    @Override
    public void setMapSource() {
        setMapViewMode(MapViewMode.CANVAS_RENDERER);
        setMapFile(Settings.getMapFile());
        if (!MapDatabase.isValidMapFile(Settings.getMapFile())) {
            Log.e("MapsforgeMapView024: Invalid map file");
        }
        Toast.makeText(
                getContext(),
                getContext().getResources().getString(R.string.warn_deprecated_mapfile),
                Toast.LENGTH_LONG)
                .show();
    }

    @Override
    public void repaintRequired(final GeneralOverlay overlay) {

        if (null == overlay) {
            invalidate();
        } else {
            try {
                final Overlay ovl = (Overlay) overlay.getOverlayImpl();

                if (ovl != null) {
                    ovl.requestRedraw();
                }

            } catch (final Exception e) {
                Log.e("MapsforgeMapView024.repaintRequired", e);
            }
        }
    }

    @Override
    public void setOnDragListener(final OnMapDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return true;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                final float distanceX, final float distanceY) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    @Override
    public boolean needsInvertedColors() {
        return false;
    }

    @Override
    public boolean hasMapThemes() {
        // not supported
        return false;
    }

    @Override
    public void setMapTheme() {
        // not supported
    }
}
