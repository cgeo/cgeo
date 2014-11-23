package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.location.Geopoint;

import org.eclipse.jdt.annotation.NonNull;

import java.util.Collection;
import java.util.List;

public interface IConnector {
    /**
     * get name for display (currently only used in links)
     *
     * @return
     */
    public String getName();

    /**
     * Check if this connector is responsible for the given geocode.
     *
     * @param geocode
     *            geocode of a cache
     * @return return {@code true}, if this connector is responsible for the cache
     */
    public boolean canHandle(final @NonNull String geocode);

    /**
     * Get the browser URL for the given cache.
     *
     * @param cache
     * @return
     */
    public String getCacheUrl(final @NonNull Geocache cache);

    /**
     * get long browser URL for the given cache
     *
     * @param cache
     * @return
     */
    public String getLongCacheUrl(final @NonNull Geocache cache);

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
    public boolean supportsFavoritePoints(final Geocache cache);

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
     * Get host name of the connector server for dynamic loading of data.
     *
     * @return
     */
    public String getHost();

    /**
     * Get cache data license text. This is displayed somewhere near the cache details.
     *
     * @param cache
     * @return
     */
    public String getLicenseText(final @NonNull Geocache cache);

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
     * Return {@code true} if this connector is active for online interaction (download details, do searches, ...). If
     * this is {@code false}, the connector will still be used for already stored offline caches.
     *
     * @return
     */

    public boolean isActive();

    /**
     * Check if the current user is the owner of the given cache.
     *
     * @param cache a cache that this connector must be able to handle
     * @return <code>true</code> if the current user is the cache owner, <code>false</code> otherwise
     */
    public boolean isOwner(final Geocache cache);

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
     * Get the list of <b>potentially</b> possible log types for a cache. Those may still be filtered further during the
     * actual logging activity.
     *
     * @param geocache
     * @return
     */
    public List<LogType> getPossibleLogTypes(Geocache geocache);

    /**
     * Get the GPX id for a waypoint when exporting. For some connectors there is an inherent name logic,
     * for others its just the 'prefix'.
     *
     * @param prefix
     * @return
     */
    public String getWaypointGpxId(String prefix, String geocode);

    /**
     * Get the 'prefix' (key) for a waypoint from the 'name' in the GPX file
     *
     * @param name
     * @return
     */
    public String getWaypointPrefix(String name);

    /**
     * Get the maximum value for Terrain
     *
     * @return
     */
    public int getMaxTerrain();

    /**
     * Get a user readable collection of all online features of this connector.
     *
     * @return
     */
    public Collection<String> getCapabilities();

    public @NonNull
    List<UserAction> getUserActions();
}
