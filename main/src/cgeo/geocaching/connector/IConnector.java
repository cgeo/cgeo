package cgeo.geocaching.connector;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IConnector {
    /**
     * get name for display. Also used for unique identification of this connector, so make sure its uniqueness!
     */
    @NonNull
    String getName();

    /**
     * Check if this connector is responsible for the given geocode.
     *
     * @param geocode geocode of a cache
     * @return return {@code true}, if this connector is responsible for the cache
     */
    boolean canHandle(@NonNull String geocode);

    /**
     * @return a couple of SQL-Like expression, applicably to a SQL geocode column to filter caches handled by this connector
     * (e.g. something like 'GC%')
     */
    @NonNull
    String[] getGeocodeSqlLikeExpressions();

    /**
     * Return a new geocodes list, with only geocodes for which this connector is responsible.
     *
     * @param geocodes list of geocodes of a cache
     * @return return a new stripped list
     */
    Set<String> handledGeocodes(@NonNull Set<String> geocodes);

    /**
     * Get the browser URL for the given cache.
     */
    @Nullable
    String getCacheUrl(@NonNull Geocache cache);

    /**
     * get long browser URL for the given cache
     */
    @Nullable
    String getLongCacheUrl(@NonNull Geocache cache);

    /**
     * Get the browser URL for the given LogEntry. May return null if no url available or identifiable.
     */
    @Nullable
    String getCacheLogUrl(@NonNull Geocache cache, @NonNull LogEntry logEntry);

    /**
     * Get a browser URL to create a new log entry. May return null if connector does not support this.
     */
    @Nullable
    String getCacheCreateNewLogUrl(@NonNull Geocache cache);

    /**
     * For a given service-log-id (as assigned by this IConnector and stored as service log id in log entries),
     * returns the logid ready for usage in scenarios such as GUI display or GPX export
     * May return null if there is no such log id.
     */
    @Nullable
    String getServiceSpecificLogId(@Nullable String serviceLogId);


    /**
     * enable/disable logging controls in cache details
     */
    boolean supportsLogging();

    /**
     * enable/disable attaching image to log
     */
    boolean supportsLogImages();

    /**
     * Get an ILoggingManager to guide the logging process.
     */
    @NonNull
    ILoggingManager getLoggingManager(@NonNull LogCacheActivity activity, @NonNull Geocache cache);

    /**
     * enable/disable changing the name of a cache
     */
    boolean supportsNamechange();

    /**
     * enable/disable changing the description of a cache
     */
    boolean supportsDescriptionchange();


    /**
     * Shall an extra description be displayed on cache detail page?
     */
    String getExtraDescription();

    /**
     * enable/disable changing found state of a cache
     */
    boolean supportsSettingFoundState();

    /**
     * Get host name of the connector server for dynamic loading of data.
     */
    @NonNull
    String getHost();

    /**
     * Return <tt>true<tt> if https must be used.
     */
    boolean isHttps();

    /**
     * Get url of the connector server for dynamic loading of data.
     *
     * @return the host prepended with "https://" or "http://" unless the host is blank, in which case the empty string is returned
     */
    @NonNull
    String getHostUrl();

    /**
     * Get url to use when testing website availability (because host url may redirect)
     */
    @NonNull
    String getTestUrl();

    /**
     * Get cache data license text. This is displayed somewhere near the cache details.
     */
    @NonNull
    String getLicenseText(@NonNull Geocache cache);

    /**
     * return true if this is a ZIP file containing a GPX file
     */
    boolean isZippedGPXFile(@NonNull String fileName);

    /**
     * extract a geocode from the given URL, if this connector can handle that URL somehow
     */
    @Nullable
    String getGeocodeFromUrl(@NonNull String url);

    /**
     * extract a geocode from the given Text, if this connector can handle that text somehow
     */
    @Nullable
    String getGeocodeFromText(@NonNull String text);

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
     */

    boolean isActive();

    /**
     * Check if the current user is the owner of the given cache.
     *
     * @param cache a cache that this connector must be able to handle
     * @return {@code true} if the current user is the cache owner, {@code false} otherwise
     */
    boolean isOwner(@NonNull Geocache cache);

    /**
     * Check if the cache information is complete enough to be
     * able to log online.
     */
    boolean canLog(@NonNull Geocache geocache);

    /**
     * Return the marker id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     */
    @DrawableRes
    int getCacheMapMarkerId();

    /**
     * Return the marker background id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     */
    @DrawableRes
    int getCacheMapMarkerBackgroundId();

    /**
     * Return the marker id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     */
    @DrawableRes
    int getCacheMapDotMarkerId();

    /**
     * Return the marker background id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     */
    @DrawableRes
    int getCacheMapDotMarkerBackgroundId();

    /**
     * Get the list of <b>potentially</b> possible log types for a cache. Those may still be filtered further during the
     * actual logging activity.
     */
    @NonNull
    List<LogType> getPossibleLogTypes(@NonNull Geocache geocache);

    /**
     * Get the GPX id for a waypoint when exporting. For some connectors there is an inherent name logic,
     * for others its just the 'prefix'.
     */
    @NonNull
    String getWaypointGpxId(@NonNull String prefix, @NonNull String geocode);

    /**
     * Get the 'prefix' (key) for a waypoint from the 'name' in the GPX file
     */
    @NonNull
    String getWaypointPrefix(String name);

    /**
     * Get the maximum value for Terrain
     */
    int getMaxTerrain();

    /**
     * Get a user readable collection of all online features of this connector.
     */
    @NonNull
    Collection<String> getCapabilities();

    @NonNull
    List<UserAction> getUserActions(UserAction.UAContext user);

    /**
     * @return the URL to register a new account or {@code null}
     */
    @Nullable
    String getCreateAccountUrl();

    /**
     * abbreviation of the connector name for shorter display, e.g. for main page login status
     */
    @NonNull
    String getNameAbbreviated();
}
