package cgeo.geocaching.mapsforge;

import org.mapsforge.android.maps.ItemizedOverlay;
import org.mapsforge.android.maps.Projection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import cgeo.geocaching.mapcommon.cgUsersOverlay;
import cgeo.geocaching.mapinterfaces.ItemizedOverlayImpl;
import cgeo.geocaching.mapinterfaces.MapProjectionImpl;
import cgeo.geocaching.mapinterfaces.MapViewImpl;

public class mfUsersOverlay extends ItemizedOverlay<mfUsersOverlayItem> implements ItemizedOverlayImpl {

	private cgUsersOverlay base;

	public mfUsersOverlay(Context contextIn, Drawable markerIn) {
		super(boundCenter(markerIn));
		base = new cgUsersOverlay(this, contextIn);
	}

	@Override
	public cgUsersOverlay getBase() {
		return base;
	}

	@Override
	protected mfUsersOverlayItem createItem(int i) {
		if (base == null)
			return null;

		return (mfUsersOverlayItem) base.createItem(i);
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
		// Nothing to do here
	}

	@Override
	public boolean superOnTap(int index) {
		return super.onTap(index);
	}

	@Override
	public void superDraw(Canvas canvas, MapViewImpl mapView, boolean shadow) {
		// Nothing to do here
	}

	@Override
	public void superDrawOverlayBitmap(Canvas canvas, Point drawPosition,
			MapProjectionImpl projection, byte drawZoomLevel) {

		super.drawOverlayBitmap(canvas, drawPosition, (Projection) projection.getImpl(), drawZoomLevel);
	}

}