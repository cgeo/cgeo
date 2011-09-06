package cgeo.geocaching.maps.google;

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
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.OtherCachersOverlay;
import cgeo.geocaching.maps.PositionOverlay;
import cgeo.geocaching.maps.ScaleOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl.overlayType;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class googleMapView extends MapView implements MapViewImpl{
	private GestureDetector gestureDetector;

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
	public CachesOverlay createAddMapOverlay(cgSettings settings,
			Context context, Drawable drawable, boolean fromDetailIntent) {

		googleCacheOverlay ovl = new googleCacheOverlay(settings, context, drawable, fromDetailIntent);
		getOverlays().add(ovl);
		return ovl.getBase();
	}

	@Override
	public OtherCachersOverlay createAddUsersOverlay(Context context, Drawable markerIn) {
		googleUsersOverlay ovl = new googleUsersOverlay(context, markerIn);
		getOverlays().add(ovl);
		return ovl.getBase();
	}

	@Override
	public PositionOverlay createAddPositionOverlay(Activity activity,
			cgSettings settingsIn) {
		
		googleOverlay ovl = new googleOverlay(activity, settingsIn, overlayType.PositionOverlay);
		getOverlays().add(ovl);
		return (PositionOverlay) ovl.getBase();
	}

	@Override
	public ScaleOverlay createAddScaleOverlay(Activity activity,
			cgSettings settingsIn) {

		googleOverlay ovl = new googleOverlay(activity, settingsIn, overlayType.ScaleOverlay);
		getOverlays().add(ovl);
		return (ScaleOverlay) ovl.getBase();
	}

	@Override
	public int getMapZoomLevel() {
		return getZoomLevel();
	}

	@Override
	public void setMapSource(cgSettings settings) {
		setSatellite(googleMapProvider.isSatelliteSourceId(settings.getMapSourceId()));
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
	public void repaintRequired(GeneralOverlay overlay) {
		invalidate();
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
	}

	@Override
	public boolean needsDarkScale() {
		return isSatellite();
	}
}
