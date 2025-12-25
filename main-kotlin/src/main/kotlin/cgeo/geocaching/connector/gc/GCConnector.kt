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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.connector.ILoggingManager
import cgeo.geocaching.connector.UserAction
import cgeo.geocaching.connector.capability.FieldNotesCapability
import cgeo.geocaching.connector.capability.IAvatar
import cgeo.geocaching.connector.capability.ICredentials
import cgeo.geocaching.connector.capability.IDifficultyTerrainMatrixNeededCapability
import cgeo.geocaching.connector.capability.IFavoriteCapability
import cgeo.geocaching.connector.capability.IIgnoreCapability
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.connector.capability.ISearchByFilter
import cgeo.geocaching.connector.capability.ISearchByGeocode
import cgeo.geocaching.connector.capability.ISearchByNextPage
import cgeo.geocaching.connector.capability.ISearchByViewPort
import cgeo.geocaching.connector.capability.PersonalNoteCapability
import cgeo.geocaching.connector.capability.PgcChallengeCheckerCapability
import cgeo.geocaching.connector.capability.Smiley
import cgeo.geocaching.connector.capability.SmileyCapability
import cgeo.geocaching.connector.capability.WatchListCapability
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.OriginGeocacheFilter
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.network.Network
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.settings.Credentials
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.sorting.GeocacheSort
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.extension.FoundNumCounter
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.TextUtils

import android.app.Activity
import android.os.Bundle

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread

import java.io.File
import java.util.Arrays
import java.util.Collection
import java.util.EnumSet
import java.util.List
import java.util.regex.Pattern

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.jetbrains.annotations.NotNull

class GCConnector : AbstractConnector() : ISearchByGeocode, ISearchByNextPage, ISearchByFilter, ISearchByViewPort, ILogin, ICredentials, FieldNotesCapability, IIgnoreCapability, WatchListCapability, PersonalNoteCapability, SmileyCapability, PgcChallengeCheckerCapability, IFavoriteCapability, IAvatar, IDifficultyTerrainMatrixNeededCapability {

    private static val MIN_RATING: Float = 1
    private static val MAX_RATING: Float = 5

    public static val SEARCH_CONTEXT_FILTER: String = "sc_gc_filter"
    public static val SEARCH_CONTEXT_SORT: String = "sc_gc_sort"
    public static val SEARCH_CONTEXT_TOOK_TOTAL: String = "sc_gc_took_total"
    public static val SEARCH_CONTEXT_BOOKMARK: String = "sc_gc_bm_id"

    private static val GC_BASE_URL: String = "https://www.geocaching.com/"

    private static val CACHE_URL_SHORT: String = "https://coord.info/"

    private static val CACHE_URL_LONG: String = GC_BASE_URL + "geocache/"
    /**
     * Pocket queries downloaded from the website use a numeric prefix. The pocket query creator Android app adds a
     * verbatim "pocketquery" prefix.
     */
    private static val GPX_ZIP_FILE_PATTERN: Pattern = Pattern.compile("((\\d{7,})|(pocketquery))" + "(_.+)?" + "\\.zip", Pattern.CASE_INSENSITIVE)

    /**
     * Pattern for GC codes
     */
    private static val PATTERN_GC_CODE: Pattern = Pattern.compile(GCConstants.GEOCODE_PATTERN, Pattern.CASE_INSENSITIVE)

    private GCConnector() {
        // singleton
        prefKey = R.string.preference_screen_gc
    }

    public static GCConnector getInstance() {
        return Holder.INSTANCE
    }

    override     public Boolean canHandle(final String geocode) {
        return PATTERN_GC_CODE.matcher(geocode).matches()
    }

    @NotNull
    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{"GC%"}
    }


    override     public String getLongCacheUrl(final Geocache cache) {
        return CACHE_URL_LONG + cache.getGeocode()
    }

    override     public String getCacheUrl(final Geocache cache) {
        return CACHE_URL_SHORT + cache.getGeocode()
    }

    override     public String getCacheLogUrl(final Geocache cache, final LogEntry logEntry) {
        if (StringUtils.isNotBlank(logEntry.serviceLogId)) {
            return CACHE_URL_SHORT + logEntry.serviceLogId
        }
        return null
    }

    override     public String getCacheCreateNewLogUrl(final Geocache cache) {
        return GC_BASE_URL + "live/geocache/" + cache.getGeocode() + "/log"
    }

    override     public Boolean canAddPersonalNote(final Geocache cache) {
        return Settings.isGCPremiumMember()
    }

    override     public Boolean supportsOwnCoordinates() {
        return true
    }

    override     public Boolean canAddToWatchList(final Geocache cache) {
        return true
    }

    override     public Boolean supportsLogging() {
        return true
    }

    override     public Boolean canEditLog(final Geocache cache, final LogEntry logEntry) {
        // needs to be online and needs to be log author
        return !StringUtils.isBlank(logEntry.serviceLogId) && logEntry.author.equalsIgnoreCase(getUserName())
    }

    override     public Boolean canDeleteLog(final Geocache cache, final LogEntry logEntry) {
        //needs to be online and needs to be log author or cache owner
        return !StringUtils.isBlank(logEntry.serviceLogId) &&
                (logEntry.author.equalsIgnoreCase(getUserName()) || cache.getOwnerUserId() == (getUserName()))
    }

    override     public Boolean supportsLogImages() {
        return true
    }

    override     public ILoggingManager getLoggingManager(final Geocache cache) {
        return GCLoggingManager(cache)
    }

    override     public Boolean canLog(final Geocache cache) {
        return StringUtils.isNotBlank(cache.getCacheId())
    }

    override     public String getName() {
        return "geocaching.com"
    }

    override     public String getNameAbbreviated() {
        return "GC"
    }

    override     public String getHost() {
        return "www.geocaching.com"
    }

    override     public String getTestUrl() {
        return "https://" + getHost() + "/play"
    }

    override     public SearchResult searchByGeocode(final String geocode, final String guid, final DisposableHandler handler) {

        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage)

        val page: String = GCParser.requestHtmlPage(geocode, guid)

        if (StringUtils.isEmpty(page)) {
            Log.e("GCConnector.searchByGeocode: No data from server")
            val search: SearchResult = SearchResult()
            search.setError(this, StatusCode.CACHE_NOT_FOUND)
            return search
        }

        val searchResult: SearchResult = GCParser.parseCache(this, page, handler)

        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCConnector.searchByGeocode: No cache parsed")
            return searchResult
        }

        // do not filter when searching for one specific cache
        return searchResult
    }

    override     public SearchResult searchByNextPage(final Bundle context) {
        // Bookmark is limited to a specific number of caches, so we can page through it without a filter
        val bmGuid: String = context.getString(SEARCH_CONTEXT_BOOKMARK)
        if (StringUtils.isNotEmpty(bmGuid)) {
            val alreadyTook: Int = context.getInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, 0)
            return GCParser.searchByBookmarkList(this, bmGuid, alreadyTook)
        }

        val filterConfig: String = context.getString(SEARCH_CONTEXT_FILTER)
        GeocacheFilter filter = null
        if (filterConfig != null) {
            filter = GeocacheFilter.createFromConfig(filterConfig)
            val origin: OriginGeocacheFilter = GeocacheFilter.findInChain(filter.getAndChainIfPossible(), OriginGeocacheFilter.class)
            if (origin != null && !origin.allowsCachesOf(this)) {
                return SearchResult()
            }
        }

        if (filter == null) {
            //we need a filter to proceed. If none is there then return empty result
            return SearchResult()
        }

        val sort: GeocacheSort = context.getParcelable(SEARCH_CONTEXT_SORT)
        return GCMap.searchByNextPage(this, context, filter, sort == null ? GeocacheSort() : sort)
    }

    override     public SearchResult searchByViewport(final Viewport viewport) {
        return searchByViewport(viewport, GeocacheFilterContext.getForType(GeocacheFilterContext.FilterType.LIVE))
    }

    override     public SearchResult searchByViewport(final Viewport viewport, final GeocacheFilter filter) {
        return GCMap.searchByViewport(this, viewport, filter)
    }


    override     public EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.of(GeocacheFilterType.DISTANCE, GeocacheFilterType.ORIGIN,
                GeocacheFilterType.NAME, GeocacheFilterType.OWNER,
                GeocacheFilterType.TYPE, GeocacheFilterType.SIZE,
                GeocacheFilterType.DIFFICULTY, GeocacheFilterType.TERRAIN, GeocacheFilterType.DIFFICULTY_TERRAIN, GeocacheFilterType.DIFFICULTY_TERRAIN_MATRIX,
                GeocacheFilterType.FAVORITES, GeocacheFilterType.STATUS, GeocacheFilterType.HIDDEN, GeocacheFilterType.EVENT_DATE, GeocacheFilterType.LOG_ENTRY)
    }


    override     public SearchResult searchByFilter(final GeocacheFilter filter, final GeocacheSort sort) {
        return GCMap.searchByFilter(this, filter, sort)
    }

    override     public Boolean isZippedGPXFile(final String fileName) {
        return GPX_ZIP_FILE_PATTERN.matcher(fileName).matches()
    }

    override     public Boolean isOwner(final Geocache cache) {
        val user: String = Settings.getUserName()
        return StringUtils.isNotEmpty(user) && StringUtils.equalsIgnoreCase(cache.getOwnerUserId(), user)
    }

    @WorkerThread
    override     public Boolean addToWatchlist(final Geocache cache) {
        val added: Boolean = GCParser.addToWatchlist(cache).blockingGet()
        if (added) {
            DataStore.saveChangedCache(cache)
        }
        return added
    }

    @WorkerThread
    override     public Boolean removeFromWatchlist(final Geocache cache) {
        val removed: Boolean = GCParser.removeFromWatchlist(cache).blockingGet()
        if (removed) {
            DataStore.saveChangedCache(cache)
        }
        return removed
    }

    /**
     * Add a cache to the favorites list.
     * <br>
     * This must not be called from the UI thread.
     *
     * @param cache the cache to add
     * @return {@code true} if the cache was successfully added, {@code false} otherwise
     */
    @WorkerThread
    override     public Boolean addToFavorites(final Geocache cache) {
        val added: Boolean = GCParser.addToFavorites(cache).blockingGet()
        if (added) {
            DataStore.saveChangedCache(cache)
        }
        return added
    }

    /**
     * Remove a cache from the favorites list.
     * <br>
     * This must not be called from the UI thread.
     *
     * @param cache the cache to add
     * @return {@code true} if the cache was successfully added, {@code false} otherwise
     */
    override     public Boolean removeFromFavorites(final Geocache cache) {
        val removed: Boolean = GCParser.removeFromFavorites(cache).blockingGet()
        if (removed) {
            DataStore.saveChangedCache(cache)
        }
        return removed
    }

    @WorkerThread
    override     public Boolean uploadModifiedCoordinates(final Geocache cache, final Geopoint wpt) {
        val uploaded: Boolean = GCParser.uploadModifiedCoordinates(cache, wpt).blockingGet()
        if (uploaded) {
            DataStore.saveChangedCache(cache)
        }
        return uploaded
    }

    @WorkerThread
    override     public Boolean deleteModifiedCoordinates(final Geocache cache) {
        val deleted: Boolean = GCParser.deleteModifiedCoordinates(cache).blockingGet()
        if (deleted) {
            DataStore.saveChangedCache(cache)
        }
        return deleted
    }

    @WorkerThread
    override     public Boolean uploadPersonalNote(final Geocache cache) {
        val uploaded: Boolean = GCParser.uploadPersonalNote(cache).blockingGet()
        if (uploaded) {
            DataStore.saveChangedCache(cache)
        }
        return uploaded
    }

    override     public Int getPersonalNoteMaxChars() {
        return 2500
    }


    override     public Boolean supportsFavoritePoints(final Geocache cache) {
        return Settings.isGCPremiumMember() && !cache.getType().isEvent() && !cache.isOwner()
    }

    override     public Boolean supportsAddToFavorite(final Geocache cache, final LogType type) {
        return cache.supportsFavoritePoints() && type.isFoundLog()
    }

    override     protected String getCacheUrlPrefix() {
        return StringUtils.EMPTY; // UNUSED
    }

    override     public String getGeocodeFromUrl(final String url) {
        // coord.info URLs
        val coordinfoUrl: String = TextUtils.getMatch(url, Pattern.compile("coord.info/" + GCConstants.GEOCODE_PATTERN, Pattern.CASE_INSENSITIVE), false, "")
        if (canHandle(coordinfoUrl)) {
            return coordinfoUrl
        }
        // expanded geocaching.com URLs
        val geocachingcomUrl: String = TextUtils.getMatch(url, Pattern.compile("geocaching.com/geocache/" + GCConstants.GEOCODE_PATTERN, Pattern.CASE_INSENSITIVE), false, "")
        if (canHandle(geocachingcomUrl)) {
            return geocachingcomUrl
        }
        return null
    }

    override     public String getGeocodeFromText(final String text) {
        // Text containing a Geocode
        val geocodeInText: String = TextUtils.getMatch(text, Pattern.compile(GCConstants.GEOCODE_PATTERN, Pattern.CASE_INSENSITIVE), false, "")
        if (canHandle(geocodeInText)) {
            return geocodeInText
        }
        return null
    }

    override     public Boolean isActive() {
        return Settings.isGCConnectorActive()
    }

    override     public Int getCacheMapMarkerId() {
        return R.drawable.marker
    }

    override     public Int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_gc
    }

    override     public Int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker
    }

    override     public Int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_gc
    }

    override     public Boolean login() {
        // login
        val status: StatusCode = GCLogin.getInstance().login()
        // update cache counter
        FoundNumCounter.getAndUpdateFoundNum(this)

        return status == StatusCode.NO_ERROR
    }

    override     public Unit logout() {
        GCLogin.getInstance().logout()
    }

    override     public String getUserName() {
        return GCLogin.getInstance().getActualUserName()
    }

    override     public Unit increaseCachesFound(final Int by) {
        GCLogin.getInstance().increaseActualCachesFound(by)
    }

    override     public Credentials getCredentials() {
        return Settings.getCredentials(R.string.pref_username, R.string.pref_password)
    }

    override     public Int getCachesFound() {
        return GCLogin.getInstance().getActualCachesFound()
    }

    override     public String getLoginStatusString() {
        return GCLogin.getInstance().getActualStatus()
    }

    override     public Boolean isLoggedIn() {
        return GCLogin.getInstance().isActualLoginStatus()
    }

    override     public String getWaypointGpxId(final String prefix, final String geocode) {
        String gpxId = prefix
        if (StringUtils.isNotBlank(geocode) && geocode.length() > 2) {
            gpxId += geocode.substring(2)
        }
        return gpxId
    }

    override     public String getWaypointPrefix(final String name) {
        String prefix = name
        if (StringUtils.isNotBlank(prefix) && prefix.length() >= 2) {
            prefix = name.substring(0, 2)
        }
        return prefix
    }

    override     public Int getUsernamePreferenceKey() {
        return R.string.pref_username
    }

    override     public Int getPasswordPreferenceKey() {
        return R.string.pref_password
    }

    override     public Int getAvatarPreferenceKey() {
        return R.string.pref_gc_avatar
    }

    override     public List<UserAction> getUserActions(final UserAction.UAContext user) {
        val actions: List<UserAction> = super.getUserActions(user)
        actions.add(UserAction(R.string.user_menu_open_browser, R.drawable.ic_menu_face, context -> ShareUtils.openUrl(context.getContext(), "https://www.geocaching.com/p/default.aspx?u=" + Network.encode(context.userName))))
        if (StringUtils.isNotBlank(user.userGUID)) {
            actions.add(UserAction(R.string.user_menu_send_message, R.drawable.ic_menu_message, context -> ShareUtils.openUrl(context.getContext(), "https://www.geocaching.com/account/messagecenter?recipientId=" + context.userGUID + (StringUtils.isNotBlank(context.geocode) ? "&gcCode=" + context.geocode : ""))))
        }
        actions.add(UserAction(R.string.user_menu_send_email, R.drawable.ic_menu_email, context -> ShareUtils.openUrl(context.getContext(), "https://www.geocaching.com/email/?u=" + Network.encode(context.userName))))
        return actions
    }

    override     public Boolean uploadFieldNotes(final File exportFile) {
        if (!GCLogin.getInstance().isActualLoginStatus()) {
            // no need to upload (possibly large file) if we're not logged in
            val loginState: StatusCode = GCLogin.getInstance().login()
            if (loginState != StatusCode.NO_ERROR) {
                Log.e("FieldNoteExport.ExportTask upload: Login failed")
                return false
            }
        }

        val uploadRes: String = Network.getResponseData(Network.postRequest("https://www.geocaching.com/api/proxy/web/v1/LogDrafts/upload", Parameters(), null, "file-0", "text/plain", exportFile))

        if (StringUtils.isBlank(uploadRes)) {
            Log.e("FieldNoteExport.ExportTask upload: No data from server")
            return false
        }
        return true
    }

    override     public Boolean canIgnoreCache(final Geocache cache) {
        return StringUtils.isNotEmpty(cache.getType().wptTypeId) && Settings.isGCPremiumMember()
    }

    override     public Boolean canRemoveFromIgnoreCache(final Geocache cache) {
        return false
    }

    override     public Boolean addToIgnorelist(final Geocache cache) {
        GCParser.ignoreCache(cache)
        return true
    }

    override     public Boolean removeFromIgnorelist(final Geocache cache) {
        // Not supported for gc.com
        return false
    }

    override     public String getCreateAccountUrl() {
        return "https://www.geocaching.com/account/register"
    }

    override     public String geMyAccountUrl() {
        return "https://www.geocaching.com/my/default.aspx"
    }

    override     public List<Smiley> getSmileys() {
        return GCSmileysProvider.getSmileys()
    }

    override     public Smiley getSmiley(final Int id) {
        return GCSmileysProvider.getSmiley(id)
    }

    override     public Boolean isChallengeCache(final Geocache cache) {
        return cache.getType() == CacheType.MYSTERY && StringUtils.containsIgnoreCase(cache.getName(), "challenge")
    }

    override     public List<LogType> getPossibleLogTypes(final Geocache geocache) {
        val result: List<LogType> = super.getPossibleLogTypes(geocache)
        // since May 2017 finding own caches is not allowed (except for events)
        if (geocache.isOwner()) {
            result.removeAll(Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, LogType.WEBCAM_PHOTO_TAKEN, LogType.NEEDS_ARCHIVE, LogType.NEEDS_MAINTENANCE))
        }
        // since May 2017 only one found log is allowed
        if (geocache.isFound()) {
            result.removeAll(Arrays.asList(LogType.FOUND_IT, LogType.ATTENDED, LogType.WEBCAM_PHOTO_TAKEN))
        }
        // since 2020 (?) needs maintenance and need archive logs are no longer separate log types (but presented in a submenu)
        result.removeAll(Arrays.asList(LogType.NEEDS_MAINTENANCE, LogType.NEEDS_ARCHIVE))
        return result
    }

    override     public Collection<ImmutablePair<Float, Float>> getNeededDifficultyTerrainCombisFor81Matrix() {
        return GCWebAPI.getNeededDifficultyTerrainCombisFor81Matrix()
    }

    override     public Boolean supportsManualLogin() {
        return GCLogin.getInstance().supportsManualLogin()
    }

    @UiThread
    override     public Unit performManualLogin(final Activity activity, final Runnable callback) {
        GCLogin.getInstance().performManualLogin(activity, callback)
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static val INSTANCE: GCConnector = GCConnector()
    }

}
