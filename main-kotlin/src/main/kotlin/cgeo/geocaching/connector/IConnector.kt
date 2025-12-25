// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector

import cgeo.geocaching.connector.capability.ICredentials
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collection
import java.util.List
import java.util.Set

interface IConnector {
    /**
     * get name for display. Also used for unique identification of this connector, so make sure its uniqueness!
     */
    String getName()

    String getDisplayName()


    /**
     * Check if this connector is responsible for the given geocode.
     *
     * @param geocode geocode of a cache
     * @return return {@code true}, if this connector is responsible for the cache
     */
    Boolean canHandle(String geocode)

    /**
     * @return a couple of SQL-Like expression, applicably to a SQL geocode column to filter caches handled by this connector
     * (e.g. something like 'GC%')
     */
    String[] getGeocodeSqlLikeExpressions()

    /**
     * Return a geocodes list, with only geocodes for which this connector is responsible.
     *
     * @param geocodes list of geocodes of a cache
     * @return return a stripped list
     */
    Set<String> handledGeocodes(Set<String> geocodes)

    /**
     * Get the browser URL for the given cache.
     */
    String getCacheUrl(Geocache cache)

    /**
     * get Long browser URL for the given cache
     */
    String getLongCacheUrl(Geocache cache)

    /**
     * Get the browser URL for the given LogEntry. May return null if no url available or identifiable.
     */
    String getCacheLogUrl(Geocache cache, LogEntry logEntry)

    /**
     * Get a browser URL to create a log entry. May return null if connector does not support this.
     */
    String getCacheCreateNewLogUrl(Geocache cache)

    /**
     * For a given service-log-id (as assigned by this IConnector and stored as service log id in log entries),
     * returns the logid ready for usage in scenarios such as GUI display or GPX export
     * May return null if there is no such log id.
     */
    String getServiceSpecificLogId(String serviceLogId)

    Int getServiceSpecificPreferenceScreenKey()

    /**
     * enable/disable logging controls in cache details
     */
    Boolean supportsLogging()

    Boolean canEditLog(Geocache cache, LogEntry logEntry)

    Boolean canDeleteLog(Geocache cache, LogEntry logEntry)

    /**
     * enable/disable attaching image to log
     */
    Boolean supportsLogImages()

    /**
     * Get an ILoggingManager to guide the logging process.
     */
    ILoggingManager getLoggingManager(Geocache cache)

    /**
     * enable/disable changing the name of a cache
     */
    Boolean supportsNamechange()

    /**
     * enable/disable changing the description of a cache
     */
    Boolean supportsDescriptionchange()


    /**
     * Shall an extra description be displayed on cache detail page?
     */
    String getExtraDescription()

    /**
     * enable/disable changing found state of a cache
     */
    Boolean supportsSettingFoundState()

    /**
     * Get host name of the connector server for dynamic loading of data.
     */
    String getHost()

    /**
     * Return <tt>true<tt> if https must be used.
     */
    Boolean isHttps()

    /**
     * Get url of the connector server for dynamic loading of data.
     *
     * @return the host prepended with "https://" or "http://" unless the host is blank, in which case the empty string is returned
     */
    String getHostUrl()

    /**
     * Get url to use when testing website availability (because host url may redirect)
     */
    String getTestUrl()

    /**
     * Get cache data license text. This is displayed somewhere near the cache details.
     */
    String getLicenseText(Geocache cache)

    /**
     * return true if this is a ZIP file containing a GPX file
     */
    Boolean isZippedGPXFile(String fileName)

    /**
     * extract a geocode from the given URL, if this connector can handle that URL somehow
     */
    String getGeocodeFromUrl(String url)

    /**
     * extract a geocode from the given Text, if this connector can handle that text somehow
     */
    String getGeocodeFromText(String text)

    /**
     * enable/disable uploading modified coordinates to website
     *
     * @return true, when uploading is possible
     */
    Boolean supportsOwnCoordinates()

    /**
     * Resetting of modified coordinates on website to details
     *
     * @return success
     */
    Boolean deleteModifiedCoordinates(Geocache cache)

    /**
     * Uploading modified coordinates to website
     *
     * @return success
     */
    Boolean uploadModifiedCoordinates(Geocache cache, Geopoint wpt)

    /**
     * Return {@code true} if this connector is active for online interaction (download details, do searches, ...). If
     * this is {@code false}, the connector will still be used for already stored offline caches.
     */

    Boolean isActive()

    /**
     * Return {@code true} if this connector is active for online interaction and has valid (stored) credentials
     */
    default Boolean hasValidCredentials() {
        return (this is ILogin && this is ICredentials && isActive() && Settings.getCredentials((ICredentials) this).isValid())
    }

    /**
     * Check if the current user is the owner of the given cache.
     *
     * @param cache a cache that this connector must be able to handle
     * @return {@code true} if the current user is the cache owner, {@code false} otherwise
     */
    Boolean isOwner(Geocache cache)

    /**
     * Check if the cache information is complete enough to be
     * able to log online.
     */
    Boolean canLog(Geocache geocache)

    /**
     * Return the marker id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     */
    @DrawableRes
    Int getCacheMapMarkerId()

    /**
     * Return the marker background id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     */
    @DrawableRes
    Int getCacheMapMarkerBackgroundId()

    /**
     * Return the marker id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     */
    @DrawableRes
    Int getCacheMapDotMarkerId()

    /**
     * Return the marker background id of the caches for this connector. This creates the different backgrounds for cache markers
     * on the map.
     */
    @DrawableRes
    Int getCacheMapDotMarkerBackgroundId()

    /**
     * Get the list of <b>potentially</b> possible log types for a cache. Those may still be filtered further during the
     * actual logging activity.
     */
    List<LogType> getPossibleLogTypes(Geocache geocache)

    /**
     * Get the GPX id for a waypoint when exporting. For some connectors there is an inherent name logic,
     * for others its just the 'prefix'.
     */
    String getWaypointGpxId(String prefix, String geocode)

    /** similar to getWaypointGpxId, but includes geocode to be distinct across different caches */
    String getFullWaypointGpxId(String prefix, String geocode)

    /**
     * Get the 'prefix' (key) for a waypoint from the 'name' in the GPX file
     */
    String getWaypointPrefix(String name)

    /**
     * Get info whether connector supports D/T ratings
     */
    Boolean supportsDifficultyTerrain()

    /**
     * Get the maximum value for Terrain
     */
    Int getMaxTerrain()

    /**
     * Get a user readable collection of all online features of this connector.
     */
    Collection<String> getCapabilities()

    List<UserAction> getUserActions(UserAction.UAContext user)

    /**
     * @return the URL to register a account or {@code null}
     */
    String getCreateAccountUrl()

    /**
     * @return the URL to an account for a user or {@code null}
     */
    String geMyAccountUrl()

    /**
     * abbreviation of the connector name for shorter display, e.g. for main page login status
     */
    String getNameAbbreviated()
}
