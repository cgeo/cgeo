package cgeo.geocaching.connector;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CancellableHandler;

public interface IConnector {
    /**
     * get name for display (currently only used in links)
     *
     * @return
     */
    public String getName();

    /**
     * return true, if this connector is responsible for the given cache
     *
     * @param geocode
     * @return
     */
    public boolean canHandle(final String geocode);

    public boolean supportsRefreshCache(final cgCache cache);

    /**
     * get browser URL for the given cache
     *
     * @param cache
     * @return
     */
    public String getCacheUrl(final cgCache cache);

    /**
     * enable/disable watchlist controls in cache details
     *
     * @return
     */
    public boolean supportsWatchList();

    /**
     * enable/disable logging controls in cache details
     *
     * @return
     */
    public boolean supportsLogging();

    /**
     * get host name of the connector server for dynamic loading of data
     *
     * @return
     */
    public String getHost();

    /**
     * get cache data license text
     *
     * @param cache
     * @return
     */
    public String getLicenseText(final cgCache cache);

    /**
     * enable/disable user actions in cache details
     *
     * @return
     */
    public boolean supportsUserActions();

    /**
     * enable/disable "caches around" action in cache details
     *
     * @return
     */
    public boolean supportsCachesAround();

    public SearchResult searchByGeocode(final String geocode, final String guid, final cgeoapplication app, final int listId, final CancellableHandler handler);

    /**
     * search caches by coordinate. must be implemented if {@link supportsCachesAround} returns <code>true</true>
     *
     * @param center
     * @return
     */
    public SearchResult searchByCoordinate(final Geopoint center);

    /**
     * Search caches by viewport.
     *
     * @param viewport
     * @param tokens
     * @return
     */
    public SearchResult searchByViewport(final Viewport viewport, final String[] tokens);

    /**
     * return true if this is a ZIP file containing a GPX file
     *
     * @param fileName
     * @return
     */
    public boolean isZippedGPXFile(final String fileName);

    /**
     * return true if coordinates of a cache are reliable. only implemented by GC connector
     *
     * @param cacheHasReliableLatLon
     *            flag of the cache
     * @return
     */
    public boolean isReliableLatLon(boolean cacheHasReliableLatLon);

    /**
     * Return required tokens for specific following actions
     *
     * @return
     */
    public String[] getTokens();

}
