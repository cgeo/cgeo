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

package cgeo.geocaching.connector.oc

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.ILoggingManager
import cgeo.geocaching.connector.UserInfo
import cgeo.geocaching.connector.UserInfo.UserInfoStatus
import cgeo.geocaching.connector.capability.IFavoriteCapability
import cgeo.geocaching.connector.capability.IIgnoreCapability
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.connector.capability.ISearchByFilter
import cgeo.geocaching.connector.capability.ISearchByNextPage
import cgeo.geocaching.connector.capability.ISearchByViewPort
import cgeo.geocaching.connector.capability.IVotingCapability
import cgeo.geocaching.connector.capability.PersonalNoteCapability
import cgeo.geocaching.connector.capability.WatchListCapability
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.sorting.GeocacheSort
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.extension.FoundNumCounter
import cgeo.geocaching.utils.CryptUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import android.os.Bundle

import androidx.annotation.NonNull
import androidx.annotation.StringRes

import java.util.EnumSet
import java.util.Locale

import org.apache.commons.lang3.StringUtils

class OCApiLiveConnector : OCApiConnector() : ISearchByViewPort, ILogin, ISearchByFilter, ISearchByNextPage, WatchListCapability, IIgnoreCapability, PersonalNoteCapability, IFavoriteCapability, IVotingCapability {

    private static val MIN_RATING: Float = 1
    private static val MAX_RATING: Float = 5
    private final String cS
    private final Int isActivePrefKeyId
    private final Int tokenPublicPrefKeyId
    private final Int tokenSecretPrefKeyId
    private var userInfo: UserInfo = UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfoStatus.NOT_RETRIEVED, UNKNOWN_FINDS)

    @SuppressWarnings("PMD.ExcessiveParameterList")
    public OCApiLiveConnector(final String name, final String host, final Boolean https, final String prefix, final String licenseString, @StringRes final Int cKResId, @StringRes final Int cSResId, final Int isActivePrefKeyId, final Int tokenPublicPrefKeyId, final Int tokenSecretPrefKeyId, final ApiSupport apiSupport, final String abbreviation, final ApiBranch apiBranch, @StringRes final Int prefKey) {
        super(name, host, https, prefix, CryptUtils.rot13(LocalizationUtils.getString(cKResId)), licenseString, apiSupport, abbreviation, apiBranch)
        this.prefKey = prefKey

        cS = CryptUtils.rot13(LocalizationUtils.getString(cSResId))
        this.isActivePrefKeyId = isActivePrefKeyId
        this.tokenPublicPrefKeyId = tokenPublicPrefKeyId
        this.tokenSecretPrefKeyId = tokenSecretPrefKeyId
    }

    override     public Boolean isActive() {
        return Settings.isOCConnectorActive(isActivePrefKeyId)
    }

    override     public SearchResult searchByViewport(final Viewport viewport) {
        val result: SearchResult = SearchResult(OkapiClient.getCachesBBox(viewport, this))

        Log.d(String.format(Locale.getDefault(), "OC returning %d caches from search by viewport", result.getCount()))

        return result
    }

    override     public OAuthLevel getSupportedAuthLevel() {

        if (Settings.hasOAuthAuthorization(tokenPublicPrefKeyId, tokenSecretPrefKeyId)) {
            return OAuthLevel.Level3
        }
        return OAuthLevel.Level1
    }

    override     public String getCS() {
        return CryptUtils.rot13(cS)
    }

    override     public Int getTokenPublicPrefKeyId() {
        return tokenPublicPrefKeyId
    }

    override     public Int getTokenSecretPrefKeyId() {
        return tokenSecretPrefKeyId
    }

    override     public Boolean canAddToWatchList(final Geocache cache) {
        return getApiSupport() == ApiSupport.current
    }

    override     public Boolean addToWatchlist(final Geocache cache) {
        val added: Boolean = OkapiClient.setWatchState(cache, true, this)

        if (added) {
            DataStore.saveChangedCache(cache)
        }

        return added
    }

    override     public Boolean removeFromWatchlist(final Geocache cache) {
        val removed: Boolean = OkapiClient.setWatchState(cache, false, this)

        if (removed) {
            DataStore.saveChangedCache(cache)
        }

        return removed
    }

    override     public Boolean supportsLogging() {
        return true
    }

    override     public ILoggingManager getLoggingManager(final Geocache cache) {
        return OkapiLoggingManager(this, cache)
    }

    override     public Boolean canLog(final Geocache cache) {
        return true
    }

    private Boolean supportsPersonalization() {
        return getSupportedAuthLevel() == OAuthLevel.Level3
    }

    override     public Boolean login() {
        if (supportsPersonalization()) {
            userInfo = OkapiClient.getUserInfo(this)
        } else {
            userInfo = UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfoStatus.NOT_SUPPORTED, UNKNOWN_FINDS)
        }
        // update cache counter
        FoundNumCounter.getAndUpdateFoundNum(this)

        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL
    }

    override     public Boolean isOwner(final Geocache cache) {
        return StringUtils.isNotEmpty(getUserName()) && StringUtils == (cache.getOwnerDisplayName(), getUserName())
    }

    override     public String getUserName() {
        return userInfo.getName()
    }

    override     public Unit increaseCachesFound(final Int by) {
        //not supported
    }


    override     public Int getCachesFound() {
        return userInfo.getFinds()
    }

    public Int getRemainingFavoritePoints() {
        return userInfo.getRemainingFavoritePoints()
    }

    override     public String getLoginStatusString() {
        return CgeoApplication.getInstance().getString(userInfo.getStatus().resId)
    }

    override     public Boolean isLoggedIn() {
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL
    }

    override     public EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.of(GeocacheFilterType.DISTANCE, GeocacheFilterType.ORIGIN,
                GeocacheFilterType.NAME, GeocacheFilterType.OWNER,
                GeocacheFilterType.TYPE, GeocacheFilterType.SIZE,
                GeocacheFilterType.DIFFICULTY, GeocacheFilterType.TERRAIN, GeocacheFilterType.DIFFICULTY_TERRAIN,
                GeocacheFilterType.RATING,
                GeocacheFilterType.FAVORITES, GeocacheFilterType.STATUS, GeocacheFilterType.LOG_ENTRY,
                GeocacheFilterType.LOGS_COUNT)
    }

    override     public SearchResult searchByFilter(final GeocacheFilter filter, final GeocacheSort sort) {
        return OkapiClient.getCachesByFilter(this, filter)
    }

    override     public SearchResult searchByNextPage(final Bundle context) {
        return OkapiClient.getCachesByNextPage(this, context)
    }

    override     public Boolean canAddPersonalNote(final Geocache cache) {
        return this.getApiSupport() == ApiSupport.current && isActive()
    }

    override     public Boolean uploadPersonalNote(final Geocache cache) {
        return OkapiClient.uploadPersonalNotes(this, cache)
    }

    override     public Int getPersonalNoteMaxChars() {
        return 20000
    }

    override     public Boolean canIgnoreCache(final Geocache cache) {
        return true
    }

    override     public Boolean canRemoveFromIgnoreCache(final Geocache cache) {
        return false
    }

    override     public Boolean addToIgnorelist(final Geocache cache) {
        val ignored: Boolean = OkapiClient.setIgnored(cache, this)

        if (ignored) {
            DataStore.saveChangedCache(cache)
        }
        return ignored
    }

    override     public Boolean removeFromIgnorelist(final Geocache cache) {
        // Not supported
        return false
    }

    override     public Boolean addToFavorites(final Geocache cache) {
        // Not supported in OKAPI
        return false
    }

    override     public Boolean removeFromFavorites(final Geocache cache) {
        // Not supported in OKAPI
        return false
    }

    override     public Boolean supportsFavoritePoints(final Geocache cache) {
        // In OKAPI is only possible to add cache as favorite when entry to log is added.
        // So setting this to false to disable favorite button that appears in cache description.
        return false
    }

    override     public Boolean supportsAddToFavorite(final Geocache cache, final LogType type) {
        return type == LogType.FOUND_IT
    }

    override     public Boolean canVote(final Geocache cache, final LogType logType) {
        return getApiBranch() == ApiBranch.ocpl && logType == LogType.FOUND_IT
    }

    override     public Boolean supportsVoting(final Geocache cache) {
        return getApiBranch() == ApiBranch.ocpl
    }

    override     public Float getRatingStep() {
        return 1f; // Only integer votes are possible
    }

    override     public Boolean isValidRating(final Float rating) {
        return ((Int) rating) == rating && rating >= MIN_RATING && rating <= MAX_RATING
    }

    override     public String getDescription(final Float rating) {
        return IVotingCapability.getDefaultFiveStarsDescription(rating)
    }

    override     public Boolean postVote(final Geocache cache, final Float rating) {
        return true; // nothing to do here for OKAPI, because vote for a cache is posted during submitting the log (postLog method).
    }
}
