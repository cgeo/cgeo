package cgeo.geocaching.maps.google;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import cgeo.geocaching.Settings;
import cgeo.geocaching.geopoint.Viewport;
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
import cgeo.geocaching.utils.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class GoogleMapView extends MapView implements MapViewImpl {
    private GestureDetector gestureDetector;
    private OnMapDragListener onDragListener;

    public GoogleMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public GoogleMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public GoogleMapView(Context context, String apiKey) {
        super(context, apiKey);
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
            Log.e("GoogleMapView.draw: " + e.toString());
        }
    }

    @Override
    public void displayZoomControls(boolean takeFocus) {
        try {
            // Push zoom controls to the right
            FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            zoomParams.gravity = Gravity.RIGHT;
            getZoomButtonsController().getZoomControls().setLayoutParams(zoomParams);

            super.displayZoomControls(takeFocus);
        } catch (Exception e) {
            Log.e("GoogleMapView.displayZoomControls: " + e.toString());
        }
    }

    @Override
    public MapControllerImpl getMapController() {
        return new GoogleMapController(getController());
    }

    @Override
    public GeoPointImpl getMapViewCenter() {
        GeoPoint point = getMapCenter();
        return new GoogleGeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
    }

    @Override
    public Viewport getViewport() {
        return new Viewport(getMapViewCenter(), getLatitudeSpan() / 1e6, getLongitudeSpan() / 1e6);
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
        return new GoogleMapProjection(getProjection());
    }

    @Override
    public CachesOverlay createAddMapOverlay(Context context, Drawable drawable) {

        GoogleCacheOverlay ovl = new GoogleCacheOverlay(context, drawable);
        getOverlays().add(ovl);
        return ovl.getBase();
    }

    @Override
    public OtherCachersOverlay createAddUsersOverlay(Context context, Drawable markerIn) {
        GoogleOtherCachersOverlay ovl = new GoogleOtherCachersOverlay(context, markerIn);
        getOverlays().add(ovl);
        return ovl.getBase();
    }

    @Override
    public PositionOverlay createAddPositionOverlay(Activity activity) {

        GoogleOverlay ovl = new GoogleOverlay(activity, overlayType.PositionOverlay);
        getOverlays().add(ovl);
        return (PositionOverlay) ovl.getBase();
    }

    @Override
    public ScaleOverlay createAddScaleOverlay(Activity activity) {

        GoogleOverlay ovl = new GoogleOverlay(activity, overlayType.ScaleOverlay);
        getOverlays().add(ovl);
        return (ScaleOverlay) ovl.getBase();
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
}
