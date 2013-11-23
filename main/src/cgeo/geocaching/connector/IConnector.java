package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;

import java.util.List;

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
    public String getCacheUrl(final Geocache cache);

    /**
     * get long browser URL for the given cache
     *
     * @param cache
     * @return
     */
    public String getLongCacheUrl(final Geocache cache);

    /**
     * enable/disable watchlist controls in cache details
     *
     * @return
     */
    public boolean supportsWatchList();

    /**
     * Add the cache to the watchlist
     *
     * @param cache
     * @return True - success/False - failure
     */
    public boolean addToWatchlist(Geocache cache);

    /**
     * Remove the cache from the watchlist
     *
     * @param cache
     * @return True - success/False - failure
     */
    public boolean removeFromWatchlist(Geocache cache);

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
     * enable/disable attaching image to log
     *
     * @return
     */
    public boolean supportsLogImages();

    /**
     * Get an ILoggingManager to guide the logging process.
     *
     * @return
     */
    public ILoggingManager getLoggingManager(final LogCacheActivity activity, final Geocache cache);

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
    public String getLicenseText(final Geocache cache);

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
     * extract a geocode from the given URL, if this connector can handle that URL somehow
     *
     * @param url
     * @return
     */
    public String getGeocodeFromUrl(final String url);

    /**
     * enable/disable uploading personal note
     *
     * @return true, when uploading is possible
     */
    public boolean supportsPersonalNote();

    /**
     * Uploading personal note to website
     *
     * @param cache
     * @return success
     */
    public boolean uploadPersonalNote(Geocache cache);

    /**
     * enable/disable uploading modified coordinates to website
     *
     * @return true, when uploading is possible
     */
    public boolean supportsOwnCoordinates();

    /**
     * Resetting of modified coordinates on website to details
     *
     * @param cache
     * @return success
     */
    public boolean deleteModifiedCoordinates(Geocache cache);

    /**
     * Uploading modified coordinates to website
     *
     * @param cache
     * @param wpt
     * @return success
     */
    public boolean uploadModifiedCoordinates(Geocache cache, Geopoint wpt);

    /**
     * Return true if this connector is activated for online
     * interaction (download details, do searches, ...)
     *
     * @return
     */

    public boolean isActivated();

    /**
     * Check if the current user is the owner of the given cache.
     *
     * @param cache a cache that this connector must be able to handle
     * @return <code>true</code> if the current user is the cache owner, <code>false</code> otherwise
     */
    public boolean isOwner(final ICache cache);

    /**
     * Check if the cache information is complete enough to be
     * able to log online.
     *
     * @param geocache
     * @return
     */
    public boolean canLog(Geocache geocache);

    /**
     * Return the marker id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     *
     * @param disabled
     *            Whether to return the enabled or disabled marker type
     */
    public int getCacheMapMarkerId(boolean disabled);

    /**
     * Get the list of <b>potentially</b> possible log types for a cache. Those may still be filter further during the
     * actual logging activity.
     *
     * @param geocache
     * @return
     */
    public List<LogType> getPossibleLogTypes(Geocache geocache);

    /**
     * Get the gpx id for a waypoint when exporting. For some connectors there is an inherent name logic,
     * for others its just the 'prefix'
     *
     * @param prefix
     * @return
     */
    public String getWaypointGpxId(String prefix, String geocode);

    /**
     * Get the 'prefix' (key) for a waypoint from the 'name' in the gpx file
     * 
     * @param name
     * @return
     */
    public String getWaypointPrefix(String name);
}
