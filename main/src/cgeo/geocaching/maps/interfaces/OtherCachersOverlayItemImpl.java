package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.go4cache.Go4CacheUser;

/**
 * Common functions of the provider-specific
 * UserOverlayItem implementations
 */
public interface OtherCachersOverlayItemImpl extends OverlayItemImpl {

    public Go4CacheUser getUser();
}
