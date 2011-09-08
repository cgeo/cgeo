package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.cgUser;

/**
 * Common functions of the provider-specific
 * UserOverlayItem implementations
 * @author rsudev
 *
 */
public interface OtherCachersOverlayItemImpl extends OverlayItemImpl {

	public cgUser getUser();
}
