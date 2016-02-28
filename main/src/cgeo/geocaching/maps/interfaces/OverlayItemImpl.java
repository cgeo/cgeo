package cgeo.geocaching.maps.interfaces;

import android.graphics.drawable.Drawable;

/**
 * Common functions of the provider-specific
 * OverlayItem implementations
 */
public interface OverlayItemImpl {

    String getTitle();

    Drawable getMarker(int index);

    void setMarker(Drawable markerIn);
}
