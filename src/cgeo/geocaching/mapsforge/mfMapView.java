package cgeo.geocaching.mapsforge;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapDatabase;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewMode;
import org.mapsforge.android.maps.Overlay;
import org.mapsforge.android.maps.Projection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.mapcommon.cgMapMyOverlay;
import cgeo.geocaching.mapcommon.cgMapOverlay;
import cgeo.geocaching.mapcommon.cgOverlayScale;
import cgeo.geocaching.mapcommon.cgUsersOverlay;
import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapControllerImpl;
import cgeo.geocaching.mapinterfaces.MapProjectionImpl;
import cgeo.geocaching.mapinterfaces.MapViewImpl;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.OverlayImpl;
import cgeo.geocaching.mapinterfaces.OverlayImpl.overlayType;

public class mfMapView extends MapView implements MapViewImpl {

	public mfMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
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
		// nothing to do here
	}

	@Override
	public MapControllerImpl getMapController() {
		return new mfMapController(getController(), getMaxZoomLevel());
	}

	@Override
	public GeoPointImpl getMapViewCenter() {
		GeoPoint point = getMapCenter();
		return new mfGeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
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
		return new mfMapProjection(getProjection());
	}

	@Override
	public cgMapOverlay createAddMapOverlay(cgSettings settings,
			Context context, Drawable drawable, boolean fromDetailIntent) {
		
		mfCacheOverlay ovl = new mfCacheOverlay(settings, context, drawable, fromDetailIntent);
		getOverlays().add(ovl);
		return ovl.getBase();
	}
	
	@Override
	public cgUsersOverlay createAddUsersOverlay(Context context, Drawable markerIn) {
		mfUsersOverlay ovl = new mfUsersOverlay(context, markerIn);
		getOverlays().add(ovl);
		return ovl.getBase();
	}

	@Override
	public cgMapMyOverlay createAddPositionOverlay(Activity activity,
			cgSettings settingsIn) {
		mfOverlay ovl = new mfOverlay(activity, settingsIn, overlayType.PositionOverlay);
		getOverlays().add(ovl);
		return (cgMapMyOverlay) ovl.getBase();
	}

	@Override
	public cgOverlayScale createAddScaleOverlay(Activity activity,
			cgSettings settingsIn) {
		mfOverlay ovl = new mfOverlay(activity, settingsIn, overlayType.ScaleOverlay);
		getOverlays().add(ovl);
		return (cgOverlayScale) ovl.getBase();
	}	

	@Override
	public int getLatitudeSpan() {
		
		int span = 0;

		Projection projection = getProjection();
		
		if (projection != null && getHeight() > 0) {
		
			GeoPoint low = projection.fromPixels(0, 0);
			GeoPoint high = projection.fromPixels(0, getHeight());

			if (low != null && high != null) {
				span = Math.abs(high.getLatitudeE6() - low.getLatitudeE6());
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
				span = Math.abs(high.getLongitudeE6() - low.getLongitudeE6());
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
		return getZoomLevel()+1;
	}

	@Override
	public boolean needsScaleOverlay() {
		return false;
	}

	@Override
	public void setBuiltinScale(boolean b) {
		setScaleBar(b);
	}
	
	@Override
	public void setMapSource(cgSettings settings) {
		
		switch(settings.mapSource) {
			case mapsforgeOsmarender:
				setMapViewMode(MapViewMode.OSMARENDER_TILE_DOWNLOAD);
				break;
			case mapsforgeCycle:
				setMapViewMode(MapViewMode.OPENCYCLEMAP_TILE_DOWNLOAD);
				break;
			case mapsforgeOffline:
				if (MapDatabase.isValidMapFile(settings.getMapFile())) {
					setMapViewMode(MapViewMode.CANVAS_RENDERER);
					super.setMapFile(settings.getMapFile());
				} else {
					setMapViewMode(MapViewMode.MAPNIK_TILE_DOWNLOAD);
				}
				break;
			default:
				setMapViewMode(MapViewMode.MAPNIK_TILE_DOWNLOAD);
		}
	}

	@Override
	public void repaintRequired(OverlayBase overlay) {
		
		try {
			mfOverlay ovl = (mfOverlay) overlay.getOverlayImpl();
			
			if (ovl != null) {
				ovl.requestRedraw();
			}
			
		} catch (Exception e) {
			Log.e(cgSettings.tag, "mfMapView.repaintRequired: " + e.toString());
		}
	}

}
