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

import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.connector.capability.IFavoriteCapability
import cgeo.geocaching.connector.capability.ISearchByFilter
import cgeo.geocaching.connector.capability.ISearchByGeocode
import cgeo.geocaching.connector.capability.ISearchByViewPort
import cgeo.geocaching.connector.capability.IVotingCapability
import cgeo.geocaching.connector.capability.PersonalNoteCapability
import cgeo.geocaching.connector.capability.WatchListCapability
import cgeo.geocaching.contacts.IContactCardProvider
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.ClipboardUtils

import android.app.Activity
import android.content.Context

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import java.util.ArrayList
import java.util.Collection
import java.util.HashSet
import java.util.List
import java.util.Set

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull

abstract class AbstractConnector : IConnector {

    @StringRes
    protected var prefKey: Int = R.string.preference_screen_services

    override     public Boolean canHandle(final String geocode) {
        return false
    }

    override     public Set<String> handledGeocodes(final Set<String> geocodes) {
        val strippedList: Set<String> = HashSet<>()
        for (final String geocode : geocodes) {
            if (canHandle(geocode)) {
                strippedList.add(geocode)
            }
        }
        return strippedList
    }

    @NotNull
    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{"%"}; //will match everything
    }

    override     public Boolean supportsOwnCoordinates() {
        return false
    }

    override     public Boolean uploadModifiedCoordinates(final Geocache cache, final Geopoint wpt) {
        throw UnsupportedOperationException()
    }

    /**
     * {@link IConnector}
     */
    override     public Boolean deleteModifiedCoordinates(final Geocache cache) {
        throw UnsupportedOperationException()
    }


    override     public Boolean supportsLogging() {
        return false
    }

    override     public Boolean canEditLog(final Geocache cache, final LogEntry logEntry) {
        return false
    }

    override     public Boolean canDeleteLog(final Geocache cache, final LogEntry logEntry) {
        return false
    }

    override     public Boolean supportsLogImages() {
        return false
    }

    override     public Boolean canLog(final Geocache cache) {
        return false
    }

    override     public ILoggingManager getLoggingManager(final Geocache cache) {
        return NoLoggingManager(this, cache)
    }

    override     public Boolean supportsNamechange() {
        return false
    }

    override     public Boolean supportsDescriptionchange() {
        return false
    }

    override     public String getExtraDescription() {
        return ""
    }

    override     public Boolean supportsSettingFoundState() {
        return false
    }

    override     public String getLicenseText(final Geocache cache) {
        return StringUtils.EMPTY
    }

    protected static Boolean isNumericId(final String str) {
        try {
            return Integer.parseInt(str) > 0
        } catch (final NumberFormatException ignored) {
        }
        return false
    }

    override     public Boolean isZippedGPXFile(final String fileName) {
        // don't accept any file by default
        return false
    }

    override     public String getGeocodeFromUrl(final String url) {
        val urlPrefix: String = getCacheUrlPrefix()
        if (StringUtils.isEmpty(urlPrefix) || StringUtils.startsWith(url, urlPrefix)) {
            val geocode: String = url.substring(urlPrefix.length())
            if (canHandle(geocode)) {
                return geocode
            }
        }
        return null
    }

    override     public String getGeocodeFromText(final String text) {
        return null
    }

    protected abstract String getCacheUrlPrefix()

    override     public Boolean isHttps() {
        return true
    }

    override     public String getHostUrl() {
        if (StringUtils.isBlank(getHost())) {
            return ""
        }
        return (isHttps() ? "https://" : "http://") + getHost()
    }

    override     public String getTestUrl() {
        return getHostUrl()
    }

    override     public String getLongCacheUrl(final Geocache cache) {
        return getCacheUrl(cache)
    }

    override     public String getCacheLogUrl(final Geocache cache, final LogEntry logEntry) {
        return null; //by default, Connector does not support log urls
    }

    override     public String getCacheCreateNewLogUrl(final Geocache cache) {
        return null; //by default, Connector does not support creating logs online
    }

    override     public String getServiceSpecificLogId(final String serviceLogId) {
        return serviceLogId; //by default, log id is directly usable
    }

    public Int getServiceSpecificPreferenceScreenKey() {
        return prefKey
    }

    override     public Boolean isActive() {
        return false
    }

    override     public Int getCacheMapMarkerId() {
        return R.drawable.marker_other
    }

    override     public Int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_other
    }

    override     public Int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker_other
    }

    override     public Int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_other
    }

    override     public List<LogType> getPossibleLogTypes(final Geocache geocache) {
        val logTypes: List<LogType> = ArrayList<>()
        if (geocache.isEventCache()) {
            logTypes.add(LogType.WILL_ATTEND)
            logTypes.add(LogType.ATTENDED)
            if (geocache.isOwner()) {
                logTypes.add(LogType.ANNOUNCEMENT)
            }
        } else if (geocache.getType() == CacheType.WEBCAM) {
            logTypes.add(LogType.WEBCAM_PHOTO_TAKEN)
        } else {
            logTypes.add(LogType.FOUND_IT)
        }
        if (!geocache.isEventCache()) {
            logTypes.add(LogType.DIDNT_FIND_IT)
        }
        logTypes.add(LogType.NOTE)
        if (!geocache.isEventCache()) {
            logTypes.add(LogType.NEEDS_MAINTENANCE)
        }
        if (geocache.isOwner()) {
            logTypes.add(LogType.OWNER_MAINTENANCE)
            if (geocache.isDisabled()) {
                logTypes.add(LogType.ENABLE_LISTING)
            } else {
                logTypes.add(LogType.TEMP_DISABLE_LISTING)
            }
            logTypes.add(LogType.ARCHIVE)
        }
        if (!geocache.isArchived() && !geocache.isOwner()) {
            logTypes.add(LogType.NEEDS_ARCHIVE)
        }
        return logTypes
    }

    override     public String getWaypointGpxId(final String prefix, final String geocode) {
        // Default: just return the prefix
        return prefix
    }

    override     public String getFullWaypointGpxId(final String prefix, final String geocode) {
        return geocode + getWaypointGpxId(prefix, geocode)
    }

    override     public String getWaypointPrefix(final String name) {
        // Default: just return the name
        return name
    }

    override     public Boolean supportsDifficultyTerrain() {
        return true
    }

    override     public Int getMaxTerrain() {
        return 5
    }

    override     public final Collection<String> getCapabilities() {
        val list: List<String> = ArrayList<>()
        addCapability(list, ISearchByViewPort.class, R.string.feature_search_live_map)
        addCapability(list, GeocacheFilterType.NAME, R.string.feature_search_keyword)
        addCapability(list, GeocacheFilterType.DISTANCE, R.string.feature_search_center)
        addCapability(list, ISearchByGeocode.class, R.string.feature_search_geocode)
        addCapability(list, GeocacheFilterType.OWNER, R.string.feature_search_owner)
        addCapability(list, GeocacheFilterType.LOG_ENTRY, R.string.feature_search_finder)
        if (supportsLogging()) {
            list.add(feature(R.string.feature_online_logging))
        }
        if (supportsLogImages()) {
            list.add(feature(R.string.feature_log_images))
        }
        addCapability(list, PersonalNoteCapability.class, R.string.feature_personal_notes)
        if (supportsOwnCoordinates()) {
            list.add(feature(R.string.feature_own_coordinates))
        }
        addCapability(list, WatchListCapability.class, R.string.feature_watch_list)
        addCapability(list, IFavoriteCapability.class, R.string.feature_favorite)
        addCapability(list, IVotingCapability.class, R.string.feature_voting)
        return list
    }

    private Unit addCapability(final List<String> capabilities, final Class<? : IConnector()> clazz, @StringRes final Int featureResourceId) {
        if (clazz.isInstance(this)) {
            capabilities.add(feature(featureResourceId))
        }
    }

    private Unit addCapability(final List<String> capabilities, final GeocacheFilterType filterType, @StringRes final Int featureResourceId) {
        if (this is ISearchByFilter && ((ISearchByFilter) this).getFilterCapabilities().contains(filterType)) {
            capabilities.add(feature(featureResourceId))
        }
    }

    private static String feature(@StringRes final Int featureResourceId) {
        return CgeoApplication.getInstance().getString(featureResourceId)
    }

    override     public List<UserAction> getUserActions(final UserAction.UAContext user) {
        val actions: List<UserAction> = getDefaultUserActions()

        if (this is ISearchByFilter) {
            val sbf: ISearchByFilter = (ISearchByFilter) this
            if (sbf.getFilterCapabilities().contains(GeocacheFilterType.OWNER)) {
                actions.add(UserAction(R.string.user_menu_view_hidden, R.drawable.ic_menu_owned, context -> CacheListActivity.startActivityOwner(context.getContext(), context.userName)))
            }
            if (sbf.getFilterCapabilities().contains(GeocacheFilterType.LOG_ENTRY)) {
                actions.add(UserAction(R.string.user_menu_view_found, R.drawable.ic_menu_emoticons, context -> CacheListActivity.startActivityFinder(context.getContext(), context.userName)))
            }
        }

        actions.add(UserAction(R.string.copy_to_clipboard, R.drawable.ic_menu_copy, context -> {
            ClipboardUtils.copyToClipboard(context.userName)
            ActivityMixin.showToast(context.getContext(), R.string.clipboard_copy_ok)
        }))
        return actions
    }

    /**
     * @return user actions which are always available (independent of cache or trackable)
     */
    public static List<UserAction> getDefaultUserActions() {
        val actions: List<UserAction> = ArrayList<>()

        actions.add(UserAction(R.string.user_menu_open_contact, R.drawable.ic_menu_contactcard, context -> {
            val ctx: Context = context.contextRef.get()
            if (ctx is IContactCardProvider && ctx is Activity) {
                ((IContactCardProvider) ctx).showContactCard(StringUtils.isBlank(context.userName) ? context.displayName : context.userName)
            }
        }))

        return actions
    }

    public Unit logout() {
    }

    public String getShortHost() {
        return StringUtils.remove(getHost(), "www.")
    }

    override     public String getCreateAccountUrl() {
        return null
    }

    override     public String geMyAccountUrl() {
        return null
    }

    override     public Boolean equals(final Object obj) {
        if (!(obj is AbstractConnector)) {
            return false
        }

        return ((AbstractConnector) obj).getName() == (this.getName())
    }

    override     public Int hashCode() {
        return getName().hashCode()
    }

    override     public String getDisplayName() {
        return getName()
    }
}
