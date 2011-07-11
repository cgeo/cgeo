package cgeo.geocaching.mapinterfaces;

import android.graphics.drawable.Drawable;

/**
 * Common functions of the provider-specific
 * OverlayItem implementations
 * @author rsudev
 *
 */
public interface OverlayItemImpl {

	public String getTitle();
	
	public Drawable getMarker(int index);
	
	public void setMarker(Drawable markerIn);
}
