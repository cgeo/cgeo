package cgeo.geocaching.mapinterfaces;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import cgeo.geocaching.mapcommon.ItemizedOverlayBase;

/**
 * Defines the common functions to access the provider-specific
 * ItemizedOverlay implementation
 * @author rsudev
 *
 */
public interface ItemizedOverlayImpl extends OverlayImpl {

	ItemizedOverlayBase getBase();

	void superPopulate();

	void superSetLastFocusedItemIndex(int i);

	Drawable superBoundCenter(Drawable markerIn);

	Drawable superBoundCenterBottom(Drawable marker);

	boolean superOnTap(int index);

	void superDraw(Canvas canvas, MapViewImpl mapView, boolean shadow);

	void superDrawOverlayBitmap(Canvas canvas, Point drawPosition, MapProjectionImpl projection,
			byte drawZoomLevel);

}
