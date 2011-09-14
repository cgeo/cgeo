package cgeo.geocaching.mapinterfaces;

import cgeo.geocaching.cgCoord;

/**
 * Covers the common functions of the provider-specific
 * CacheOverlayItem implementations
 * 
 * @author rsudev
 * 
 */
public interface CacheOverlayItemImpl extends OverlayItemImpl {

    public cgCoord getCoord();

    public String getType();

}
