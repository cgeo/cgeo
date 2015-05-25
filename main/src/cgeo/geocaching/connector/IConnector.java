package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.location.Geopoint;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public interface IConnector {
    /**
     * get name for display (currently only used in links)
     *
     */
    @NonNull
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
     */
    @Nullable
    public String getCacheUrl(final @NonNull Geocache cache);

    /**
     * get long browser URL for the given cache
     *
     */
    @Nullable
    public String getLongCacheUrl(final @NonNull Geocache cache);

    /**
     * enable/disable watchlist controls in cache details
     *
     */
    public boolean supportsWatchList();

    /**
     * Add the cache to the watchlist
     *
     * @return True - success/False - failure
     */
    public boolean addToWatchlist(@NonNull Geocache cache);

    /**
     * Remove the cache from the watchlist
     *
     * @return True - success/False - failure
     */
    public boolean removeFromWatchlist(@NonNull Geocache cache);

    /**
     * enable/disable favorite points controls in cache details
     *
     */
    public boolean supportsFavoritePoints(@NonNull final Geocache cache);

    /**
     * enable/disable logging controls in cache details
     *
     */
    public boolean supportsLogging();

    /**
     * enable/disable attaching image to log
     *
     */
    public boolean supportsLogImages();

    /**
     * Get an ILoggingManager to guide the logging process.
     *
     */
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final Geocache cache);

    /**
     * Get host name of the connector server for dynamic loading of data.
     *
     */
    @NonNull
    public String getHost();

    /**
     * Get cache data license text. This is displayed somewhere near the cache details.
     *
     */
    @NonNull
    public String getLicenseText(final @NonNull Geocache cache);

    /**
     * return true if this is a ZIP file containing a GPX file
     *
     */
    public boolean isZippedGPXFile(@NonNull final String fileName);

    /**
     * return true if coordinates of a cache are reliable. only implemented by GC connector
     *
     * @param cacheHasReliableLatLon
     *            flag of the cache
     */
    public boolean isReliableLatLon(boolean cacheHasReliableLatLon);

    /**
     * extract a geocode from the given URL, if this connector can handle that URL somehow
     *
     */
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url);

    /**
     * enable/disable uploading personal note
     *
     * @return true, when uploading is possible
     */
    public boolean supportsPersonalNote();

    /**
     * Uploading personal note to website
     *
     * @return success
     */
    public boolean uploadPersonalNote(@NonNull Geocache cache);

    /**
     * enable/disable uploading modified coordinates to website
     *
     * @return true, when uploading is possible
     */
    public boolean supportsOwnCoordinates();

    /**
     * Resetting of modified coordinates on website to details
     *
     * @return success
     */
    public boolean deleteModifiedCoordinates(@NonNull Geocache cache);

    /**
     * Uploading modified coordinates to website
     *
     * @return success
     */
    public boolean uploadModifiedCoordinates(@NonNull Geocache cache, @NonNull Geopoint wpt);

    /**
     * Return {@code true} if this connector is active for online interaction (download details, do searches, ...). If
     * this is {@code false}, the connector will still be used for already stored offline caches.
     *
     */

    public boolean isActive();

    /**
     * Check if the current user is the owner of the given cache.
     *
     * @param cache a cache that this connector must be able to handle
     * @return <code>true</code> if the current user is the cache owner, <code>false</code> otherwise
     */
    public boolean isOwner(@NonNull final Geocache cache);

    /**
     * Check if the cache information is complete enough to be
     * able to log online.
     *
     */
    public boolean canLog(@NonNull Geocache geocache);

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
     */
    @NonNull
    public List<LogType> getPossibleLogTypes(@NonNull Geocache geocache);

    /**
     * Get the GPX id for a waypoint when exporting. For some connectors there is an inherent name logic,
     * for others its just the 'prefix'.
     *
     */
    public String getWaypointGpxId(String prefix, @NonNull String geocode);

    /**
     * Get the 'prefix' (key) for a waypoint from the 'name' in the GPX file
     *
     */
    @NonNull
    public String getWaypointPrefix(String name);

    /**
     * Get the maximum value for Terrain
     *
     */
    public int getMaxTerrain();

    /**
     * Get a user readable collection of all online features of this connector.
     *
     */
    @NonNull
    public Collection<String> getCapabilities();

    @NonNull
    public List<UserAction> getUserActions();
}
