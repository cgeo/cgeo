package cgeo.geocaching.mapsforge;

import org.mapsforge.android.maps.ItemizedOverlay;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.mapcommon.cgMapOverlay;
import cgeo.geocaching.mapinterfaces.ItemizedOverlayImpl;
import cgeo.geocaching.mapinterfaces.MapProjectionImpl;
import cgeo.geocaching.mapinterfaces.MapViewImpl;


public class mfCacheOverlay extends ItemizedOverlay<mfCacheOverlayItem> implements ItemizedOverlayImpl {

	private cgMapOverlay base;

	public mfCacheOverlay(cgSettings settingsIn, Context contextIn, Drawable markerIn, Boolean fromDetailIn) {
		super(boundCenterBottom(markerIn));
		base = new cgMapOverlay(settingsIn, this, contextIn, fromDetailIn);
	}
	
	@Override
	public cgMapOverlay getBase() {
		return base;
	}

	@Override
	protected mfCacheOverlayItem createItem(int i) {
		if (base == null)
			return null;

		return (mfCacheOverlayItem) base.createItem(i);
	}

	@Override
	public int size() {
		if (base == null)
			return 0;

		return base.size();
	}

	@Override
	protected boolean onTap(int arg0) {
		if (base == null)
			return false;
		
		return base.onTap(arg0);
	}

	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition,
			Projection projection, byte drawZoomLevel) {
		base.drawOverlayBitmap(canvas, drawPosition, new mfMapProjection(projection), drawZoomLevel);
	}
	
	@Override
	public void superPopulate() {
		populate();
	}

	@Override
	public Drawable superBoundCenter(Drawable markerIn) {
		return super.boundCenter(markerIn);
	}

	@Override
	public Drawable superBoundCenterBottom(Drawable marker) {
		return super.boundCenterBottom(marker);
	}

	@Override
	public void superSetLastFocusedItemIndex(int i) {
		// nothing to do
	}

	@Override
	public boolean superOnTap(int index) {
		return super.onTap(index);
	}

	@Override
	public void superDraw(Canvas canvas, MapViewImpl mapView, boolean shadow) {
		// nothing to do here...
	}

	@Override
	public void superDrawOverlayBitmap(Canvas canvas, Point drawPosition,
			MapProjectionImpl projection, byte drawZoomLevel) {
		super.drawOverlayBitmap(canvas, drawPosition, (Projection) projection.getImpl(), drawZoomLevel);
	}

}

