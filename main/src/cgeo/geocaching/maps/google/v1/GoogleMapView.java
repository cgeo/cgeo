package cgeo.geocaching.maps.google.v1;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.PositionAndScaleOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ZoomButtonsController;

public class GoogleMapView extends MapView implements MapViewImpl {
    private GestureDetector gestureDetector;
    private OnMapDragListener onDragListener;
    private final GoogleMapController mapController = new GoogleMapController(getController());

    public GoogleMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public GoogleMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }

    public GoogleMapView(Context context, String apiKey) {
        super(context, apiKey);
        initialize(context);
    }

    private void initialize(Context context) {
        if (isInEditMode()) {
            return;
        }
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public void draw(final Canvas canvas) {
        try {
            if (getMapZoomLevel() > 22) { // to avoid too close zoom level (mostly on Samsung Galaxy S series)
                getController().setZoom(22);
            }

            super.draw(canvas);
        } catch (Exception e) {
            Log.e("GoogleMapView.draw", e);
        }
    }

    @Override
    public void displayZoomControls(boolean takeFocus) {
        try {
            // Push zoom controls to the right
            FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            zoomParams.gravity = Gravity.RIGHT;
            // The call to retrieve the zoom buttons controller is undocumented and works so far on all devices
            // supported by Google Play, but fails at least on one Jolla.
            final ZoomButtonsController controller = (ZoomButtonsController) MethodUtils.invokeMethod(this, "getZoomButtonsController");
            controller.getZoomControls().setLayoutParams(zoomParams);

            super.displayZoomControls(takeFocus);
        } catch (NoSuchMethodException ignored) {
            Log.w("GoogleMapView.displayZoomControls: unable to explicitly place the zoom buttons");
        } catch (Exception e) {
            Log.e("GoogleMapView.displayZoomControls", e);
        }
    }

    @Override
    public MapControllerImpl getMapController() {
        return mapController;
    }

    @Override
    @NonNull
    public GeoPointImpl getMapViewCenter() {
        GeoPoint point = getMapCenter();
        return new GoogleGeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
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
        return new GoogleMapProjection(getProjection());
    }

    @Override
    public CachesOverlay createAddMapOverlay(Context context, Drawable drawable) {

        GoogleCacheOverlay ovl = new GoogleCacheOverlay(context, drawable);
        getOverlays().add(ovl);
        return ovl.getBase();
    }

    @Override
    public PositionAndScaleOverlay createAddPositionAndScaleOverlay() {

        GoogleOverlay ovl = new GoogleOverlay();
        getOverlays().add(ovl);
        return (PositionAndScaleOverlay) ovl.getBase();
    }

    @Override
    public int getMapZoomLevel() {
        return getZoomLevel();
    }

    @Override
    public void setMapSource() {
        setSatellite(GoogleMapProvider.isSatelliteSource(Settings.getMapSource()));
    }

    @Override
    public void repaintRequired(GeneralOverlay overlay) {
        invalidate();
    }

    @Override
    public void setOnDragListener(OnMapDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            gestureDetector.onTouchEvent(ev);
            return super.onTouchEvent(ev);
        } catch (Exception e) {
            Log.e("GoogleMapView.onTouchEvent", e);
        }
        return false;
    }

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            getController().zoomInFixing((int) e.getX(), (int) e.getY());
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

    @Override
    public boolean hasMapThemes() {
        // Not supported
        return false;
    }

    @Override
    public void setMapTheme() {
        // Not supported
    }
}
