package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.maps.CacheMarker;

/**
 * Common functions of the provider-specific
 * OverlayItem implementations
 */
public interface OverlayItemImpl {

    String getTitle();

    CacheMarker getMarker(int index);

    void setMarker(CacheMarker markerIn);
}
