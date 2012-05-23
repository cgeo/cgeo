package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.enumerations.CacheType;

/**
 * Covers the common functions of the provider-specific
 * CacheOverlayItem implementations
 */
public interface CachesOverlayItemImpl extends OverlayItemImpl {

    public IWaypoint getCoord();

    public CacheType getType();

}
