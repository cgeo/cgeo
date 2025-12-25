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

package cgeo.geocaching.connector.su

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.connector.ILoggingManager
import cgeo.geocaching.connector.UserInfo
import cgeo.geocaching.connector.UserInfo.UserInfoStatus
import cgeo.geocaching.connector.capability.IFavoriteCapability
import cgeo.geocaching.connector.capability.IIgnoreCapability
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.connector.capability.IOAuthCapability
import cgeo.geocaching.connector.capability.ISearchByFilter
import cgeo.geocaching.connector.capability.ISearchByGeocode
import cgeo.geocaching.connector.capability.ISearchByViewPort
import cgeo.geocaching.connector.capability.IVotingCapability
import cgeo.geocaching.connector.capability.PersonalNoteCapability
import cgeo.geocaching.connector.capability.WatchListCapability
import cgeo.geocaching.connector.oc.OCApiConnector.OAuthLevel
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.sorting.GeocacheSort
import cgeo.geocaching.storage.extension.FoundNumCounter
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.EnumSet
import java.util.List

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull

class SuConnector : AbstractConnector() : ISearchByGeocode, ISearchByViewPort, ILogin, IOAuthCapability, WatchListCapability, PersonalNoteCapability, ISearchByFilter, IFavoriteCapability, IVotingCapability, IIgnoreCapability {

    private static val PREFIX_MULTISTEP_VIRTUAL: CharSequence = "MV"
    private static val PREFIX_TRADITIONAL: CharSequence = "TR"
    private static val PREFIX_VIRTUAL: CharSequence = "VI"
    private static val PREFIX_MULTISTEP: CharSequence = "MS"
    private static val PREFIX_EVENT: CharSequence = "EV"
    private static val PREFIX_CONTEST: CharSequence = "CT"
    private static val PREFIX_MYSTERY: CharSequence = "LT"
    private static val PREFIX_MYSTERY_VIRTUAL: CharSequence = "LV"

    // Let just add this general prefix, since for search prefix is not important at all,
    // all IDs are unique at SU
    private static val PREFIX_GENERAL: CharSequence = "SU"

    private var userInfo: UserInfo = UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfoStatus.NOT_RETRIEVED)

    private SuConnector() {
        // singleton
        prefKey = R.string.preference_screen_su
    }

    public static SuConnector getInstance() {
        return Holder.INSTANCE
    }

    /**
     * For geocaching.su geocode is not immutable because first two letters
     * indicate cache type, which may change. However ID is immutable, that's why it's preferred
     * to use ID in all the places
     *
     * @param geocode cache's geocode like MV15736
     * @return cache ID, i.e. 15736
     */
    public static String geocodeToId(final String geocode) {
        return geocode.substring(2)
    }

    public OAuthLevel getSupportedAuthLevel() {
        if (hasAuthorization()) {
            return OAuthLevel.Level3
        }
        return OAuthLevel.Level1
    }

    public Boolean hasAuthorization() {
        return Settings.hasOAuthAuthorization(getTokenPublicPrefKeyId(), getTokenSecretPrefKeyId())
    }

    private Boolean supportsPersonalization() {
        return getSupportedAuthLevel() == OAuthLevel.Level3
    }

    override     public Boolean login() {
        if (supportsPersonalization()) {
            try {
                userInfo = SuApi.getUserInfo(this)
            } catch (final SuApi.SuApiException e) {
                userInfo = UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfoStatus.FAILED)
            }
        } else {
            userInfo = UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfo.UserInfoStatus.NOT_SUPPORTED)
        }
        // update cache counter
        FoundNumCounter.getAndUpdateFoundNum(this)

        return userInfo.getStatus() == UserInfo.UserInfoStatus.SUCCESSFUL
    }

    override     public String getUserName() {
        return userInfo.getName()
    }

    override     public Int getCachesFound() {
        return userInfo.getFinds()
    }

    override     public Unit increaseCachesFound(final Int by) {
        //not supported
    }

    override     public String getLoginStatusString() {
        return CgeoApplication.getInstance().getString(userInfo.getStatus().resId)
    }

    override     public Boolean isLoggedIn() {
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL
    }

    override     public String getName() {
        return "Geocaching.su"
    }

    override     public String getNameAbbreviated() {
        return "GC.SU"
    }

    override     public String getCacheUrl(final Geocache cache) {
        return getCacheUrlPrefix() + "/" + cache.getCacheId()
    }

    override     public String getCacheLogUrl(final Geocache cache, final LogEntry logEntry) {
        if (StringUtils.isNotBlank(logEntry.serviceLogId)) {
            return getCacheUrl(cache) + "#p" + logEntry.serviceLogId
        }
        return null
    }


    override     public String getHost() {
        return "geocaching.su"
    }

    override     public Boolean isOwner(final Geocache cache) {
        return StringUtils.isNotEmpty(getUserName()) && StringUtils == (cache.getOwnerDisplayName(), getUserName())
    }

    override     public Boolean canHandle(final String geocode) {
        return StringUtils.startsWithAny(StringUtils.upperCase(geocode), PREFIX_GENERAL, PREFIX_TRADITIONAL, PREFIX_MULTISTEP_VIRTUAL, PREFIX_VIRTUAL, PREFIX_MULTISTEP, PREFIX_EVENT, PREFIX_CONTEST, PREFIX_MYSTERY, PREFIX_MYSTERY_VIRTUAL) && isNumericId(SuConnector.geocodeToId(geocode))
    }

    @NotNull
    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{PREFIX_GENERAL + "%", PREFIX_TRADITIONAL + "%", PREFIX_MULTISTEP_VIRTUAL + "%", PREFIX_VIRTUAL + "%", PREFIX_MULTISTEP + "%", PREFIX_EVENT + "%", PREFIX_CONTEST + "%", PREFIX_MYSTERY + "%", PREFIX_MYSTERY_VIRTUAL + "%"}
    }

    override     protected String getCacheUrlPrefix() {
        return getHostUrl() + "/cache"
    }

    override     public Boolean isActive() {
        return Settings.isSUConnectorActive()
    }

    public final String getConsumerKey() {
        return CgeoApplication.getInstance().getString(R.string.su_consumer_key)
    }

    public final String getConsumerSecret() {
        return CgeoApplication.getInstance().getString(R.string.su_consumer_secret)
    }

    override     public Int getTokenPublicPrefKeyId() {
        return R.string.pref_su_tokenpublic
    }

    override     public Int getTokenSecretPrefKeyId() {
        return R.string.pref_su_tokensecret
    }

    override     public SearchResult searchByGeocode(final String geocode, final String guid, final DisposableHandler handler) {
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage)

        final Geocache cache
        try {
            cache = SuApi.searchByGeocode(geocode)
        } catch (final SuApi.CacheNotFoundException e) {
            return SearchResult(this, StatusCode.CACHE_NOT_FOUND)
        } catch (final SuApi.NotAuthorizedException e) {
            return SearchResult(this, StatusCode.NOT_LOGGED_IN)
        } catch (final SuApi.ConnectionErrorException e) {
            return SearchResult(this, StatusCode.CONNECTION_FAILED_SU)
        } catch (final Exception e) {
            Log.e("SuConnector.searchByGeocode failed: ", e)
            return null
        }

        return SearchResult(cache)
    }

    override     public SearchResult searchByViewport(final Viewport viewport) {
        try {
            return SearchResult(SuApi.searchByBBox(viewport, this))
        } catch (final SuApi.NotAuthorizedException e) {
            return SearchResult(this, StatusCode.NOT_LOGGED_IN)
        } catch (final SuApi.ConnectionErrorException e) {
            return SearchResult(this, StatusCode.CONNECTION_FAILED_SU)
        } catch (final Exception e) {
            Log.e("SuConnector.searchByViewport failed: ", e)
            return SearchResult(this, StatusCode.UNKNOWN_ERROR)
        }
    }

    override     public EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.of(GeocacheFilterType.DISTANCE, GeocacheFilterType.ORIGIN, GeocacheFilterType.NAME, GeocacheFilterType.OWNER)
    }

    override     public SearchResult searchByFilter(final GeocacheFilter filter, final GeocacheSort sort) {
        try {
            return SearchResult(SuApi.searchByFilter(filter, this))
        } catch (final SuApi.NotAuthorizedException e) {
            return SearchResult(this, StatusCode.NOT_LOGGED_IN)
        } catch (final SuApi.ConnectionErrorException e) {
            return SearchResult(this, StatusCode.CONNECTION_FAILED_SU)
        } catch (final Exception e) {
            Log.e("SuConnector.searchByFilter failed: ", e)
            return SearchResult(this, StatusCode.UNKNOWN_ERROR)
        }
    }

    override     public List<LogType> getPossibleLogTypes(final Geocache geocache) {
        val logTypes: List<LogType> = ArrayList<>()

        val isOwner: Boolean = geocache.isOwner()

        if (!isOwner) {
            logTypes.add(LogType.FOUND_IT)
            logTypes.add(LogType.DIDNT_FIND_IT)
        }

        logTypes.add(LogType.NOTE)

        if (isOwner) {
            logTypes.add(LogType.OWNER_MAINTENANCE)
        }
        return logTypes
    }

    override     public Boolean supportsLogging() {
        return true
    }

    override     public Boolean supportsLogImages() {
        return true
    }

    override     public ILoggingManager getLoggingManager(final Geocache cache) {
        return SuLoggingManager(this, cache)
    }

    override     public Boolean canLog(final Geocache cache) {
        return true
    }

    override     public Boolean canAddToWatchList(final Geocache cache) {
        return true
    }

    override     public Boolean addToWatchlist(final Geocache cache) {
        return SuApi.setWatchState(cache, true)
    }

    override     public Boolean removeFromWatchlist(final Geocache cache) {
        return SuApi.setWatchState(cache, false)
    }

    /**
     * Whether or not the connector supports adding a note to a specific cache. In most cases the argument will not be
     * relevant.
     *
     * @param cache geocache
     */
    override     public Boolean canAddPersonalNote(final Geocache cache) {
        return true
    }

    /**
     * Upload personal note (already stored as member of the cache) to the connector website.
     *
     * @param cache geocache
     * @return success
     */
    override     public Boolean uploadPersonalNote(final Geocache cache) {
        return SuApi.uploadPersonalNote(cache)
    }

    /**
     * Returns the maximum number of characters allowed in personal notes.
     *
     * @return max number of characters
     */
    override     public Int getPersonalNoteMaxChars() {
        return 9500
    }

    /**
     * Add the cache to favorites
     *
     * @param cache
     * @return True - success/False - failure
     */
    override     public Boolean addToFavorites(final Geocache cache) {
        return SuApi.setRecommendation(cache, true)
    }

    /**
     * Remove the cache from favorites
     *
     * @param cache
     * @return True - success/False - failure
     */
    override     public Boolean removeFromFavorites(final Geocache cache) {
        return SuApi.setRecommendation(cache, false)
    }

    /**
     * enable/disable favorite points controls in cache details
     *
     * @param cache
     */
    override     public Boolean supportsFavoritePoints(final Geocache cache) {
        return !cache.isOwner()
    }

    /**
     * Check whether to show favorite controls during logging for the given log type
     *
     * @param cache a cache that this connector must be able to handle
     * @param type  a log type selected by the user
     * @return true, when cache can be added to favorite
     */
    override     public Boolean supportsAddToFavorite(final Geocache cache, final LogType type) {
        return type == LogType.FOUND_IT && cache.supportsFavoritePoints()
    }

    /**
     * Returns the web address to create an account
     *
     * @return web address
     */
    override     public String getCreateAccountUrl() {
        return StringUtils.join(getHostUrl(), "/register")
    }

    override     public Boolean canVote(final Geocache cache, final LogType logType) {
        return logType == LogType.FOUND_IT
    }

    override     public Boolean supportsVoting(final Geocache cache) {
        return true
    }

    override     public Float getRatingStep() {
        return 1f; // Only integer votes are possible
    }

    override     public Boolean isValidRating(final Float rating) {
        // Only integer votes are accepted
        return ((Int) rating) == rating
    }

    override     public String getDescription(final Float rating) {
        return IVotingCapability.getDefaultFiveStarsDescription(rating)
    }

    /**
     * Posts single request to update the vote only.
     *
     * @param cache  cache to vote for
     * @param rating vote to set
     * @return status of the request
     */
    override     public Boolean postVote(final Geocache cache, final Float rating) {
        return SuApi.postVote(cache, rating)
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static val INSTANCE: SuConnector = SuConnector()
    }

    override     public Boolean canIgnoreCache(final Geocache cache) {
        return true
    }

    override     public Boolean addToIgnorelist(final Geocache cache) {
        SuApi.setIgnoreState(cache, true)
        return true
    }


    override     public Boolean canRemoveFromIgnoreCache(final Geocache cache) {
        return true
    }

    override     public Boolean removeFromIgnorelist(final Geocache cache) {
        SuApi.setIgnoreState(cache, false)
        return true
    }

}
