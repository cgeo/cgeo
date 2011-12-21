package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.cgCoord;
import cgeo.geocaching.enumerations.CacheType;

/**
 * Covers the common functions of the provider-specific
 * CacheOverlayItem implementations
 *
 * @author rsudev
 *
 */
public interface CachesOverlayItemImpl extends OverlayItemImpl {

    public cgCoord getCoord();

    public CacheType getType();

}
