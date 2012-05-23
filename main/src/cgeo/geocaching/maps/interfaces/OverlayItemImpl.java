package cgeo.geocaching.maps.interfaces;

import android.graphics.drawable.Drawable;

/**
 * Common functions of the provider-specific
 * OverlayItem implementations
 */
public interface OverlayItemImpl {

    public String getTitle();

    public Drawable getMarker(int index);

    public void setMarker(Drawable markerIn);
}
