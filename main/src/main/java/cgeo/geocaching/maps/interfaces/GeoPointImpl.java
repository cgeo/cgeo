package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.models.ICoordinate;

/**
 * Defines the common functions of the provider-specific
 * GeoPoint implementations
 */
public interface GeoPointImpl extends ICoordinate {

    int getLatitudeE6();

    int getLongitudeE6();

}
