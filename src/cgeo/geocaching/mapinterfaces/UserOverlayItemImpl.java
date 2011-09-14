package cgeo.geocaching.mapinterfaces;

import cgeo.geocaching.cgUser;

/**
 * Common functions of the provider-specific
 * UserOverlayItem implementations
 * 
 * @author rsudev
 * 
 */
public interface UserOverlayItemImpl extends OverlayItemImpl {

    public cgUser getUser();
}
