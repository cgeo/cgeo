package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheRealm;
import cgeo.geocaching.geopoint.Geopoint;

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
     * enable/disable favorite points controls in cache details
     *
     * @return
     */
    public boolean supportsFavoritePoints();

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

    /**
     * extract a geocode from the given URL, if this connector can handle that URL somehow
     *
     * @param url
     * @return
     */
    public String getGeocodeFromUrl(final String url);

    /**
     * enable/disable uploading modified coordinates to website
     *
     * @return true, when uploading is possible
     */
    public boolean supportsOwnCoordinates();

    /**
     * Uploading modified coordinates to website
     *
     * @param cache
     * @param wpt
     * @return success
     */
    public boolean uploadModifiedCoordinates(cgCache cache, Geopoint wpt);

    /**
     * Reseting of modified coordinates on website to details
     *
     * @param cache
     * @return success
     */
    public boolean deleteModifiedCoordinates(cgCache cache);

    /**
     * The CacheRealm this cache belongs to
     *
     * @return
     */
    public CacheRealm getCacheRealm();

    /**
     * Return true if this connector is activated for online
     * interaction (download details, do searches, ...)
     *
     * @return
     */
    public boolean isActivated();
}
