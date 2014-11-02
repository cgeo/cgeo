package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.PositionAndScaleOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorFactory;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorInternal;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.core.GeoPoint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
public class MapsforgeMapView extends MapView implements MapViewImpl {
    private GestureDetector gestureDetector;
    private OnMapDragListener onDragListener;
    private final MapsforgeMapController mapController = new MapsforgeMapController(getController(), getMapGenerator().getZoomLevelMax());

    public MapsforgeMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(Context context) {
        if (isInEditMode()) {
            return;
        }
        gestureDetector = new GestureDetector(context, new GestureListener());
        if (Settings.isScaleMapsforgeText()) {
            this.setTextScale(getResources().getDisplayMetrics().density);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        try {
            // Google Maps and OSM Maps use different zoom levels for the same view.
            // Here we don't want the Google Maps compatible zoom level, but the actual one.
            if (getActualMapZoomLevel() > 22) { // to avoid too close zoom level (mostly on Samsung Galaxy S series)
                getController().setZoom(22);
            }

            super.draw(canvas);
        } catch (Exception e) {
            Log.e("MapsforgeMapView.draw", e);
        }
    }

    @Override
    public void displayZoomControls(boolean takeFocus) {
        // nothing to do here
    }

    @Override
    public MapControllerImpl getMapController() {
        return mapController;
    }

    @Override
    @NonNull
    public GeoPointImpl getMapViewCenter() {
        GeoPoint point = getMapPosition().getMapCenter();
        return new MapsforgeGeoPoint(point.latitudeE6, point.longitudeE6);
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
    public CachesOverlay createAddMapOverlay(Context context, Drawable drawable) {

        MapsforgeCacheOverlay ovl = new MapsforgeCacheOverlay(context, drawable);
        getOverlays().add(ovl);
        return ovl.getBase();
    }

    @Override
    public PositionAndScaleOverlay createAddPositionAndScaleOverlay() {
        MapsforgeOverlay ovl = new MapsforgeOverlay();
        getOverlays().add(ovl);
        return (PositionAndScaleOverlay) ovl.getBase();
    }

    @Override
    public int getLatitudeSpan() {

        int span = 0;

        Projection projection = getProjection();

        if (projection != null && getHeight() > 0) {

            GeoPoint low = projection.fromPixels(0, 0);
            GeoPoint high = projection.fromPixels(0, getHeight());

            if (low != null && high != null) {
                span = Math.abs(high.latitudeE6 - low.latitudeE6);
            }
        }

        return span;
    }

    @Override
    public int getLongitudeSpan() {

        int span = 0;

        Projection projection = getProjection();

        if (projection != null && getWidth() > 0) {
            GeoPoint low = projection.fromPixels(0, 0);
            GeoPoint high = projection.fromPixels(getWidth(), 0);

            if (low != null && high != null) {
                span = Math.abs(high.longitudeE6 - low.longitudeE6);
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
        return getMapPosition().getZoomLevel() + 1;
    }

    /**
     * Get the actual map zoom level
     *
     * @return the current map zoom level with no adjustments
     */
    private int getActualMapZoomLevel() {
        return getMapPosition().getZoomLevel();
    }

    @Override
    public void setMapSource() {

        MapGeneratorInternal newMapType = MapGeneratorInternal.MAPNIK;
        final MapSource mapSource = Settings.getMapSource();
        if (mapSource instanceof MapsforgeMapSource) {
            newMapType = ((MapsforgeMapSource) mapSource).getGenerator();
        }

        MapGenerator mapGenerator = MapGeneratorFactory.createMapGenerator(newMapType);

        // When swapping map sources, make sure we aren't exceeding max zoom. See bug #1535
        final int maxZoom = mapGenerator.getZoomLevelMax();
        if (getMapPosition().getZoomLevel() > maxZoom) {
            getController().setZoom(maxZoom);
        }

        setMapGenerator(mapGenerator);
        if (!mapGenerator.requiresInternetConnection()) {
            if (!new File(Settings.getMapFile()).exists()) {
                Toast.makeText(
                        getContext(),
                        getContext().getResources().getString(R.string.warn_nonexistant_mapfile),
                        Toast.LENGTH_LONG)
                        .show();
                return;
            }
            setMapFile(new File(Settings.getMapFile()));
            if (!Settings.isValidMapFile(Settings.getMapFile())) {
                Toast.makeText(
                        getContext(),
                        getContext().getResources().getString(R.string.warn_invalid_mapfile),
                        Toast.LENGTH_LONG)
                        .show();
            }
        }
        if (hasMapThemes()) {
            setMapTheme();
        }
    }

    @Override
    public boolean hasMapThemes() {
        return !getMapGenerator().requiresInternetConnection();
    }

    @Override
    public void setMapTheme() {
        String customRenderTheme = Settings.getCustomRenderThemeFilePath();
        if (StringUtils.isNotEmpty(customRenderTheme)) {
            try {
                setRenderTheme(new File(customRenderTheme));
            } catch (FileNotFoundException ignored) {
                Toast.makeText(
                        getContext(),
                        getContext().getResources().getString(R.string.warn_rendertheme_missing),
                        Toast.LENGTH_LONG)
                        .show();
            }
        } else {
            setRenderTheme(DEFAULT_RENDER_THEME);
        }
    }

    @Override
    public void repaintRequired(GeneralOverlay overlay) {

        if (null == overlay) {
            invalidate();
        } else {
            try {
                Overlay ovl = (Overlay) overlay.getOverlayImpl();

                if (ovl != null) {
                    ovl.requestRedraw();
                }

            } catch (Exception e) {
                Log.e("MapsforgeMapView.repaintRequired", e);
            }
        }
    }

    @Override
    public void setOnDragListener(OnMapDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
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
}
