package cgeo.geocaching.maps.interfaces;

import android.graphics.Point;

/**
 * Defines common functions of the provider-specific
 * MapProjection implementations
 */
public interface MapProjectionImpl {

    Object getImpl();

    void toPixels(GeoPointImpl leftGeo, Point left);

}
