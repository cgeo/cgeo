package cgeo.geocaching.apps;

import cgeo.geocaching.cgCache;

public interface App {
    public boolean isInstalled();

    public boolean isDefaultNavigationApp();

    public String getName();

    int getId();

    /**
     * whether or not the app can be used with the given cache (may depend on properties of the cache)
     *
     * @param cache
     * @return
     */
    boolean isEnabled(final cgCache cache);
}
