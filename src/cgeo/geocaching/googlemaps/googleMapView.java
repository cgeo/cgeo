package cgeo.geocaching.googlemaps;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.mapcommon.cgMapMyOverlay;
import cgeo.geocaching.mapcommon.cgMapOverlay;
import cgeo.geocaching.mapcommon.cgOverlayScale;
import cgeo.geocaching.mapcommon.cgUsersOverlay;
import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapControllerImpl;
import cgeo.geocaching.mapinterfaces.MapProjectionImpl;
import cgeo.geocaching.mapinterfaces.MapViewImpl;
import cgeo.geocaching.mapinterfaces.OnDragListener;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.OverlayImpl;
import cgeo.geocaching.mapinterfaces.OverlayImpl.overlayType;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class googleMapView extends MapView implements MapViewImpl {
	private GestureDetector gestureDetector;
	private OnDragListener onDragListener;

	public googleMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		gestureDetector = new GestureDetector(context, new GestureListener());
	}

	public googleMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		gestureDetector = new GestureDetector(context, new GestureListener());
	}

	public googleMapView(Context context, String apiKey) {
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
			Log.e(cgSettings.tag, "cgMapView.draw: " + e.toString());
		}
	}

	@Override
	public void displayZoomControls(boolean takeFocus) {
		try {
			super.displayZoomControls(takeFocus);
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgMapView.displayZoomControls: " + e.toString());
		}
	}

	@Override
	public MapControllerImpl getMapController() {
		return new googleMapController(getController());
	}

	@Override
	public GeoPointImpl getMapViewCenter() {
		GeoPoint point = getMapCenter();
		return new googleGeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
	}

	@Override
	public void addOverlay(OverlayImpl ovl) {
		getOverlays().add((Overlay)ovl);
	}

	@Override
	public void clearOverlays() {
		getOverlays().clear();
	}

	@Override
	public MapProjectionImpl getMapProjection() {
		return new googleMapProjection(getProjection());
	}

	@Override
	public cgMapOverlay createAddMapOverlay(cgSettings settings,
			Context context, Drawable drawable, boolean fromDetailIntent) {

		googleCacheOverlay ovl = new googleCacheOverlay(settings, context, drawable, fromDetailIntent);
		getOverlays().add(ovl);
		return ovl.getBase();
	}

	@Override
	public cgUsersOverlay createAddUsersOverlay(Context context, Drawable markerIn) {
		googleUsersOverlay ovl = new googleUsersOverlay(context, markerIn);
		getOverlays().add(ovl);
		return ovl.getBase();
	}

	@Override
	public cgMapMyOverlay createAddPositionOverlay(Activity activity,
			cgSettings settingsIn) {
		
		googleOverlay ovl = new googleOverlay(activity, settingsIn, overlayType.PositionOverlay);
		getOverlays().add(ovl);
		return (cgMapMyOverlay) ovl.getBase();
	}

	@Override
	public cgOverlayScale createAddScaleOverlay(Activity activity,
			cgSettings settingsIn) {

		googleOverlay ovl = new googleOverlay(activity, settingsIn, overlayType.ScaleOverlay);
		getOverlays().add(ovl);
		return (cgOverlayScale) ovl.getBase();
	}

	@Override
	public int getMapZoomLevel() {
		return getZoomLevel();
	}

	@Override
	public void setMapSource(cgSettings settings) {

		switch(settings.mapSource) {
			case googleSat:
				setSatellite(true);
				break;
			default:
				setSatellite(false);
		}
	}

	@Override
	public boolean needsScaleOverlay() {
		return true;
	}

	@Override
	public void setBuiltinScale(boolean b) {
		//Nothing to do for google maps...
	}

	@Override
	public void repaintRequired(OverlayBase overlay) {
		invalidate();
	}

	@Override
	public void setOnDragListener(OnDragListener onDragListener) {
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
			getController().zoomInFixing((int) e.getX(), (int) e.getY());
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
}
