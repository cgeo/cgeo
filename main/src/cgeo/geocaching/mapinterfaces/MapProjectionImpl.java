package cgeo.geocaching.mapinterfaces;

import android.graphics.Point;

/**
 * Defines common functions of the provider-specific
 * MapProjection implementations
 *
 * @author rsudev
 *
 */
public interface MapProjectionImpl {

    Object getImpl();

    void toPixels(GeoPointImpl leftGeo, Point left);

}
