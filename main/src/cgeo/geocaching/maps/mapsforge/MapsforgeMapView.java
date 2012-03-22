package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.Settings;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.OtherCachersOverlay;
import cgeo.geocaching.maps.PositionOverlay;
import cgeo.geocaching.maps.ScaleOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.maps.interfaces.OverlayImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl.overlayType;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorFactory;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorInternal;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.core.GeoPoint;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import java.io.File;
public class MapsforgeMapView extends MapView implements MapViewImpl {
    private GestureDetector gestureDetector;
    private OnMapDragListener onDragListener;
    private MapGeneratorInternal mapType;

    public MapsforgeMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public void draw(Canvas canvas) {
        try {
            if (getMapZoomLevel() >= 22) { // to avoid too close zoom level (mostly on Samsung Galaxy S series)
                getController().setZoom(22);
            }

            super.draw(canvas);
        } catch (Exception e) {
            Log.e(Settings.tag, "MapsforgeMapView.draw: " + e.toString());
        }
    }

    @Override
    public void displayZoomControls(boolean takeFocus) {
        // nothing to do here
    }

    @Override
    public MapControllerImpl getMapController() {
        return new MapsforgeMapController(getController(), getMapGenerator().getZoomLevelMax());
    }

    @Override
    public GeoPointImpl getMapViewCenter() {
        GeoPoint point = getMapPosition().getMapCenter();
        return new MapsforgeGeoPoint(point.latitudeE6, point.longitudeE6);
    }

    @Override
    public void addOverlay(OverlayImpl ovl) {
        getOverlays().add((Overlay) ovl);
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
    public OtherCachersOverlay createAddUsersOverlay(Context context, Drawable markerIn) {
        MapsforgeOtherCachersOverlay ovl = new MapsforgeOtherCachersOverlay(context, markerIn);
        getOverlays().add(ovl);
        return ovl.getBase();
    }

    @Override
    public PositionOverlay createAddPositionOverlay(Activity activity) {
        MapsforgeOverlay ovl = new MapsforgeOverlay(activity, overlayType.PositionOverlay);
        getOverlays().add(ovl);
        return (PositionOverlay) ovl.getBase();
    }

    @Override
    public ScaleOverlay createAddScaleOverlay(Activity activity) {
        MapsforgeOverlay ovl = new MapsforgeOverlay(activity, overlayType.ScaleOverlay);
        getOverlays().add(ovl);
        return (ScaleOverlay) ovl.getBase();
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

    @Override
    public int getMapZoomLevel() {
        return getMapPosition().getZoomLevel() + 1;
    }

    @Override
    public void setMapSource() {

        MapGeneratorInternal newMapType = MapGeneratorInternal.MAPNIK;
        switch (MapsforgeMapProvider.getMapsforgeSource(Settings.getMapSource())) {
            case MapsforgeMapProvider.CYCLEMAP:
                newMapType = MapGeneratorInternal.OPENCYCLEMAP;
                break;
            case MapsforgeMapProvider.OFFLINE:
                if (MapsforgeMapProvider.isValidMapFile(Settings.getMapFile())) {
                    newMapType = MapGeneratorInternal.DATABASE_RENDERER;
                } else {
                    newMapType = MapGeneratorInternal.MAPNIK;
                }
                break;
            default:
                newMapType = MapGeneratorInternal.MAPNIK;
        }
            mapGenerator = MapGeneratorFactory.createMapGenerator(newMapType);
            setMapGenerator(mapGenerator);
            if (!mapGenerator.requiresInternetConnection()) {
                setMapFile(new File(Settings.getMapFile()));
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
                Log.e(Settings.tag, "MapsforgeMapView.repaintRequired: " + e.toString());
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
