package cgeo.geocaching.connector;

import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IConnector {
    /**
     * get name for display (currently only used in links)
     *
     */
    @NonNull
    String getName();

    /**
     * Check if this connector is responsible for the given geocode.
     *
     * @param geocode
     *            geocode of a cache
     * @return return {@code true}, if this connector is responsible for the cache
     */
    boolean canHandle(@NonNull final String geocode);

    /**
     * Return a new geocodes list, with only geocodes for which this connector is responsible.
     *
     * @param geocodes
     *            list of geocodes of a cache
     * @return return a new stripped list
     */
    Set<String> handledGeocodes(@NonNull final Set<String> geocodes);

    /**
     * Get the browser URL for the given cache.
     *
     */
    @Nullable
    String getCacheUrl(@NonNull final Geocache cache);

    /**
     * get long browser URL for the given cache
     *
     */
    @Nullable
    String getLongCacheUrl(@NonNull final Geocache cache);

    /**
     * enable/disable favorite points controls in cache details
     *
     */
    boolean supportsFavoritePoints(@NonNull final Geocache cache);

    /**
     * enable/disable logging controls in cache details
     *
     */
    boolean supportsLogging();

    /**
     * enable/disable attaching image to log
     *
     */
    boolean supportsLogImages();

    /**
     * Get an ILoggingManager to guide the logging process.
     *
     */
    @NonNull
    ILoggingManager getLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final Geocache cache);

    /**
     * Get host name of the connector server for dynamic loading of data.
     *
     */
    @NonNull
    String getHost();

    /**
     * Get url of the connector server for dynamic loading of data.
     *
     */
    @NonNull
    String getHostUrl();

    /**
     * Get url to use when testing website availability (because host url may redirect)
     *
     */
    @NonNull
    String getTestUrl();

    /**
     * Get cache data license text. This is displayed somewhere near the cache details.
     *
     */
    @NonNull
    String getLicenseText(@NonNull final Geocache cache);

    /**
     * return true if this is a ZIP file containing a GPX file
     *
     */
    boolean isZippedGPXFile(@NonNull final String fileName);

    /**
     * return true if coordinates of a cache are reliable. only implemented by GC connector
     *
     * @param cacheHasReliableLatLon
     *            flag of the cache
     */
    boolean isReliableLatLon(boolean cacheHasReliableLatLon);

    /**
     * extract a geocode from the given URL, if this connector can handle that URL somehow
     *
     */
    @Nullable
    String getGeocodeFromUrl(@NonNull final String url);

    /**
     * enable/disable uploading modified coordinates to website
     *
     * @return true, when uploading is possible
     */
    boolean supportsOwnCoordinates();

    /**
     * Resetting of modified coordinates on website to details
     *
     * @return success
     */
    boolean deleteModifiedCoordinates(@NonNull Geocache cache);

    /**
     * Uploading modified coordinates to website
     *
     * @return success
     */
    boolean uploadModifiedCoordinates(@NonNull Geocache cache, @NonNull Geopoint wpt);

    /**
     * Return {@code true} if this connector is active for online interaction (download details, do searches, ...). If
     * this is {@code false}, the connector will still be used for already stored offline caches.
     *
     */

    boolean isActive();

    /**
     * Check if the current user is the owner of the given cache.
     *
     * @param cache a cache that this connector must be able to handle
     * @return {@code true} if the current user is the cache owner, {@code false} otherwise
     */
    boolean isOwner(@NonNull final Geocache cache);

    /**
     * Check if the cache information is complete enough to be
     * able to log online.
     *
     */
    boolean canLog(@NonNull Geocache geocache);

    /**
     * Return the marker id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     *
     * @param disabled
     *            Whether to return the enabled or disabled marker type
     */
    int getCacheMapMarkerId(boolean disabled);

    /**
     * Get the list of <b>potentially</b> possible log types for a cache. Those may still be filtered further during the
     * actual logging activity.
     *
     */
    @NonNull
    List<LogType> getPossibleLogTypes(@NonNull Geocache geocache);

    /**
     * Get the GPX id for a waypoint when exporting. For some connectors there is an inherent name logic,
     * for others its just the 'prefix'.
     *
     */
    String getWaypointGpxId(String prefix, @NonNull String geocode);

    /**
     * Get the 'prefix' (key) for a waypoint from the 'name' in the GPX file
     *
     */
    @NonNull
    String getWaypointPrefix(String name);

    /**
     * Get the maximum value for Terrain
     *
     */
    int getMaxTerrain();

    /**
     * Get a user readable collection of all online features of this connector.
     *
     */
    @NonNull
    Collection<String> getCapabilities();

    @NonNull
    List<UserAction> getUserActions();

    /**
     * Check cache is eligible for adding to favorite
     *
     * @param cache
     *         a cache that this connector must be able to handle
     * @param type
     *         a log type selected by the user
     * @return true, when cache can be added to favorite
     */
    boolean supportsAddToFavorite(final Geocache cache, final LogType type);

    /**
     * @return the URL to register a new account or {@code null}
     */
    @Nullable
    String getCreateAccountUrl();
}
