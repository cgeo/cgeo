package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.models.INamedGeoCoordinate;

/**
 * Covers the common functions of the provider-specific
 * CacheOverlayItem implementations
 */
public interface CachesOverlayItemImpl extends OverlayItemImpl {

    INamedGeoCoordinate getCoord();

    boolean applyDistanceRule();
}
