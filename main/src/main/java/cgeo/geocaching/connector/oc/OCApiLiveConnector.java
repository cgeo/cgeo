package cgeo.geocaching.connector.oc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.UserInfo;
import cgeo.geocaching.connector.UserInfo.UserInfoStatus;
import cgeo.geocaching.connector.capability.IFavoriteCapability;
import cgeo.geocaching.connector.capability.IIgnoreCapability;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByFilter;
import cgeo.geocaching.connector.capability.ISearchByNextPage;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.GeocacheSort;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.FoundNumCounter;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.EnumSet;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class OCApiLiveConnector extends OCApiConnector implements ISearchByViewPort, ILogin, ISearchByFilter, ISearchByNextPage, WatchListCapability, IIgnoreCapability, PersonalNoteCapability, IFavoriteCapability, IVotingCapability {

    private static final float MIN_RATING = 1;
    private static final float MAX_RATING = 5;
    private final String cS;
    private final int isActivePrefKeyId;
    private final int tokenPublicPrefKeyId;
    private final int tokenSecretPrefKeyId;
    private UserInfo userInfo = new UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfoStatus.NOT_RETRIEVED, UNKNOWN_FINDS);

    @SuppressWarnings("PMD.ExcessiveParameterList")
    public OCApiLiveConnector(final String name, final String host, final boolean https, final String prefix, final String licenseString, @StringRes final int cKResId, @StringRes final int cSResId, final int isActivePrefKeyId, final int tokenPublicPrefKeyId, final int tokenSecretPrefKeyId, final ApiSupport apiSupport, @NonNull final String abbreviation, final ApiBranch apiBranch, @StringRes final int prefKey) {
        super(name, host, https, prefix, CryptUtils.rot13(LocalizationUtils.getString(cKResId)), licenseString, apiSupport, abbreviation, apiBranch);
        this.prefKey = prefKey;

        cS = CryptUtils.rot13(LocalizationUtils.getString(cSResId));
        this.isActivePrefKeyId = isActivePrefKeyId;
        this.tokenPublicPrefKeyId = tokenPublicPrefKeyId;
        this.tokenSecretPrefKeyId = tokenSecretPrefKeyId;
    }

    @Override
    public boolean isActive() {
        return Settings.isOCConnectorActive(isActivePrefKeyId);
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport) {
        final SearchResult result = new SearchResult(OkapiClient.getCachesBBox(viewport, this));

        Log.d(String.format(Locale.getDefault(), "OC returning %d caches from search by viewport", result.getCount()));

        return result;
    }

    @Override
    public OAuthLevel getSupportedAuthLevel() {

        if (Settings.hasOAuthAuthorization(tokenPublicPrefKeyId, tokenSecretPrefKeyId)) {
            return OAuthLevel.Level3;
        }
        return OAuthLevel.Level1;
    }

    @Override
    public String getCS() {
        return CryptUtils.rot13(cS);
    }

    @Override
    public int getTokenPublicPrefKeyId() {
        return tokenPublicPrefKeyId;
    }

    @Override
    public int getTokenSecretPrefKeyId() {
        return tokenSecretPrefKeyId;
    }

    @Override
    public boolean canAddToWatchList(@NonNull final Geocache cache) {
        return getApiSupport() == ApiSupport.current;
    }

    @Override
    public boolean addToWatchlist(@NonNull final Geocache cache) {
        final boolean added = OkapiClient.setWatchState(cache, true, this);

        if (added) {
            DataStore.saveChangedCache(cache);
        }

        return added;
    }

    @Override
    public boolean removeFromWatchlist(@NonNull final Geocache cache) {
        final boolean removed = OkapiClient.setWatchState(cache, false, this);

        if (removed) {
            DataStore.saveChangedCache(cache);
        }

        return removed;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final Geocache cache) {
        return new OkapiLoggingManager(this, cache);
    }

    @Override
    public boolean canLog(@NonNull final Geocache cache) {
        return true;
    }

    private boolean supportsPersonalization() {
        return getSupportedAuthLevel() == OAuthLevel.Level3;
    }

    @Override
    public boolean login() {
        if (supportsPersonalization()) {
            userInfo = OkapiClient.getUserInfo(this);
        } else {
            userInfo = new UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfoStatus.NOT_SUPPORTED, UNKNOWN_FINDS);
        }
        // update cache counter
        FoundNumCounter.getAndUpdateFoundNum(this);

        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL;
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return StringUtils.isNotEmpty(getUserName()) && StringUtils.equals(cache.getOwnerDisplayName(), getUserName());
    }

    @Override
    public String getUserName() {
        return userInfo.getName();
    }

    @Override
    public void increaseCachesFound(final int by) {
        //not supported
    }


    @Override
    public int getCachesFound() {
        return userInfo.getFinds();
    }

    public int getRemainingFavoritePoints() {
        return userInfo.getRemainingFavoritePoints();
    }

    @Override
    public String getLoginStatusString() {
        return CgeoApplication.getInstance().getString(userInfo.getStatus().resId);
    }

    @Override
    public boolean isLoggedIn() {
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL;
    }

    @NonNull
    @Override
    public EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.of(GeocacheFilterType.DISTANCE, GeocacheFilterType.ORIGIN,
                GeocacheFilterType.NAME, GeocacheFilterType.OWNER,
                GeocacheFilterType.TYPE, GeocacheFilterType.SIZE,
                GeocacheFilterType.DIFFICULTY, GeocacheFilterType.TERRAIN, GeocacheFilterType.DIFFICULTY_TERRAIN,
                GeocacheFilterType.RATING,
                GeocacheFilterType.FAVORITES, GeocacheFilterType.STATUS, GeocacheFilterType.LOG_ENTRY,
                GeocacheFilterType.LOGS_COUNT);
    }

    @NonNull
    @Override
    public SearchResult searchByFilter(@NonNull final GeocacheFilter filter, @NonNull final GeocacheSort sort) {
        return OkapiClient.getCachesByFilter(this, filter);
    }

    @NonNull
    @Override
    public SearchResult searchByNextPage(final Bundle context) {
        return OkapiClient.getCachesByNextPage(this, context);
    }

    @Override
    public boolean canAddPersonalNote(@NonNull final Geocache cache) {
        return this.getApiSupport() == ApiSupport.current && isActive();
    }

    @Override
    public boolean uploadPersonalNote(@NonNull final Geocache cache) {
        return OkapiClient.uploadPersonalNotes(this, cache);
    }

    @Override
    public int getPersonalNoteMaxChars() {
        return 20000;
    }

    @Override
    public boolean canIgnoreCache(@NonNull final Geocache cache) {
        return true;
    }

    @Override
    public boolean canRemoveFromIgnoreCache(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean addToIgnorelist(@NonNull final Geocache cache) {
        final boolean ignored = OkapiClient.setIgnored(cache, this);

        if (ignored) {
            DataStore.saveChangedCache(cache);
        }
        return ignored;
    }

    @Override
    public boolean removeFromIgnorelist(@NonNull final Geocache cache) {
        // Not supported
        return false;
    }

    @Override
    public boolean addToFavorites(@NonNull final Geocache cache) {
        // Not supported in OKAPI
        return false;
    }

    @Override
    public boolean removeFromFavorites(@NonNull final Geocache cache) {
        // Not supported in OKAPI
        return false;
    }

    @Override
    public boolean supportsFavoritePoints(@NonNull final Geocache cache) {
        // In OKAPI is only possible to add cache as favorite when new entry to log is added.
        // So setting this to false to disable favorite button that appears in cache description.
        return false;
    }

    @Override
    public boolean supportsAddToFavorite(@NonNull final Geocache cache, final LogType type) {
        return type == LogType.FOUND_IT;
    }

    @Override
    public boolean canVote(@NonNull final Geocache cache, @NonNull final LogType logType) {
        return getApiBranch() == ApiBranch.ocpl && logType == LogType.FOUND_IT;
    }

    @Override
    public boolean supportsVoting(@NonNull final Geocache cache) {
        return getApiBranch() == ApiBranch.ocpl;
    }

    @Override
    public float getRatingStep() {
        return 1f; // Only integer votes are possible
    }

    @Override
    public boolean isValidRating(final float rating) {
        return ((int) rating) == rating && rating >= MIN_RATING && rating <= MAX_RATING;
    }

    @Override
    public String getDescription(final float rating) {
        return IVotingCapability.getDefaultFiveStarsDescription(rating);
    }

    @Override
    public boolean postVote(@NonNull final Geocache cache, final float rating) {
        return true; // nothing to do here for OKAPI, because vote for a cache is posted during submitting the log (postLog method).
    }
}
