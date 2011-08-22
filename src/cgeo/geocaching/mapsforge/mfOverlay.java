package cgeo.geocaching.mapsforge;

import org.mapsforge.android.maps.Overlay;
import org.mapsforge.android.maps.Projection;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Point;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.mapcommon.cgMapMyOverlay;
import cgeo.geocaching.mapcommon.cgOverlayScale;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.OverlayImpl;

public class mfOverlay extends Overlay implements OverlayImpl {

	private OverlayBase overlayBase = null;
	
	public mfOverlay(Activity activityIn, cgSettings settingsIn, OverlayImpl.overlayType ovlType) {
		
		switch (ovlType) {
			case PositionOverlay:
				overlayBase = new cgMapMyOverlay(settingsIn, activityIn, this);
				break;
			case ScaleOverlay:
				overlayBase = new cgOverlayScale(activityIn, settingsIn, this);
		}
	}
	
	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition,
			Projection projection, byte drawZoomLevel) {
		
		if (overlayBase != null) {
			overlayBase.drawOverlayBitmap(canvas, drawPosition, new mfMapProjection(projection), drawZoomLevel);
		}
	}

	public OverlayBase getBase() {
		return overlayBase;
	}
}
