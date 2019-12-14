package cgeo.geocaching.connector.su;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.UserInfo;
import cgeo.geocaching.connector.UserInfo.UserInfoStatus;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.IOAuthCapability;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.connector.oc.OCApiConnector.OAuthLevel;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class SuConnector extends AbstractConnector implements ISearchByCenter, ISearchByGeocode, ISearchByViewPort, ILogin, IOAuthCapability, WatchListCapability, PersonalNoteCapability {

    private static final CharSequence PREFIX_MULTISTEP_VIRTUAL = "MV";
    private static final CharSequence PREFIX_TRADITIONAL = "TR";
    private static final CharSequence PREFIX_VIRTUAL = "VI";
    private static final CharSequence PREFIX_MULTISTEP = "MS";
    private static final CharSequence PREFIX_EVENT = "EV";
    private static final CharSequence PREFIX_CONTEST = "CT";
    private static final CharSequence PREFIX_MYSTERY = "LT";
    private static final CharSequence PREFIX_MYSTERY_VIRTUAL = "LV";

    // Let just add this general prefix, since for search prefix is not important at all,
    // all IDs are unique at SU
    private static final CharSequence PREFIX_GENERAL = "SU";

    private UserInfo userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfoStatus.NOT_RETRIEVED);

    private SuConnector() {
        // singleton
    }

    public static SuConnector getInstance() {
        return Holder.INSTANCE;
    }

    public OAuthLevel getSupportedAuthLevel() {
        if (hasAuthorization()) {
            return OAuthLevel.Level3;
        }
        return OAuthLevel.Level1;
    }

    public boolean hasAuthorization() {
        return Settings.hasOAuthAuthorization(getTokenPublicPrefKeyId(), getTokenSecretPrefKeyId());
    }

    private boolean supportsPersonalization() {
        return getSupportedAuthLevel() == OAuthLevel.Level3;
    }

    @Override
    public boolean login(final Handler handler, @Nullable final Activity fromActivity) {
        if (supportsPersonalization()) {
            try {
                userInfo = SuApi.getUserInfo(this);
            } catch (final SuApi.SuApiException e) {
                userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfoStatus.FAILED);
            }
        } else {
            userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfo.UserInfoStatus.NOT_SUPPORTED);
        }
        return userInfo.getStatus() == UserInfo.UserInfoStatus.SUCCESSFUL;
    }

    @Override
    public String getUserName() {
        return userInfo.getName();
    }

    @Override
    public int getCachesFound() {
        return userInfo.getFinds();
    }

    @Override
    public String getLoginStatusString() {
        return CgeoApplication.getInstance().getString(userInfo.getStatus().resId);
    }

    @Override
    public boolean isLoggedIn() {
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL;
    }

    @Override
    @NonNull
    public String getName() {
        return "Geocaching.su";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return "GC.SU";
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + "&cid=" + cache.getCacheId();
    }

    @Override
    public boolean getHttps() {
        return true;
    }

    @Override
    @NonNull
    public String getHost() {
        return "geocaching.su";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return StringUtils.isNotEmpty(getUserName()) && StringUtils.equals(cache.getOwnerDisplayName(), getUserName());
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return StringUtils.startsWithAny(StringUtils.upperCase(geocode), PREFIX_GENERAL, PREFIX_TRADITIONAL, PREFIX_MULTISTEP_VIRTUAL, PREFIX_VIRTUAL, PREFIX_MULTISTEP, PREFIX_EVENT, PREFIX_CONTEST, PREFIX_MYSTERY, PREFIX_MYSTERY_VIRTUAL) && isNumericId(SuConnector.geocodeToId(geocode));
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return getHostUrl() + "/?pn=101";
    }

    @Override
    public boolean isActive() {
        return Settings.isSUConnectorActive();
    }

    public final String getConsumerKey() {
        return CgeoApplication.getInstance().getString(R.string.su_consumer_key);
    }

    public final String getConsumerSecret() {
        return CgeoApplication.getInstance().getString(R.string.su_consumer_secret);
    }

    @Override
    public int getTokenPublicPrefKeyId() {
        return R.string.pref_su_tokenpublic;
    }

    @Override
    public int getTokenSecretPrefKeyId() {
        return R.string.pref_su_tokensecret;
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final DisposableHandler handler) {
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final Geocache cache;
        try {
            cache = SuApi.searchByGeocode(geocode);
        } catch (final SuApi.CacheNotFoundException e) {
            return new SearchResult(StatusCode.CACHE_NOT_FOUND);
        } catch (final SuApi.NotAuthorizedException e) {
            return new SearchResult(StatusCode.NOT_LOGGED_IN);
        } catch (final SuApi.ConnectionErrorException e) {
            return new SearchResult(StatusCode.CONNECTION_FAILED_SU);
        } catch (final Exception e) {
            Log.e("SuConnector.searchByGeocode failed: ", e);
            return null;
        }

        return new SearchResult(cache);
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport) {
        try {
            return new SearchResult(SuApi.searchByBBox(viewport, this));
        } catch (final SuApi.NotAuthorizedException e) {
            return new SearchResult(StatusCode.NOT_LOGGED_IN);
        } catch (final SuApi.ConnectionErrorException e) {
            return new SearchResult(StatusCode.CONNECTION_FAILED_SU);
        } catch (final Exception e) {
            Log.e("SuConnector.searchByViewport failed: ", e);
            return new SearchResult(StatusCode.UNKNOWN_ERROR);
        }
    }

    @Override
    @NonNull
    public SearchResult searchByCenter(@NonNull final Geopoint center) {
        try {
            return new SearchResult(SuApi.searchByCenter(center, 20, this));
        } catch (final SuApi.NotAuthorizedException e) {
            return new SearchResult(StatusCode.NOT_LOGGED_IN);
        } catch (final SuApi.ConnectionErrorException e) {
            return new SearchResult(StatusCode.CONNECTION_FAILED_SU);
        } catch (final Exception e) {
            Log.e("SuConnector.searchByCenter failed: ", e);
            return new SearchResult(StatusCode.UNKNOWN_ERROR);
        }

    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes(@NonNull final Geocache geocache) {
        final List<LogType> logTypes = new ArrayList<>();

        final boolean isOwner = geocache.isOwner();

        if (!isOwner) {
            logTypes.add(LogType.FOUND_IT);
            logTypes.add(LogType.DIDNT_FIND_IT);
        }

        logTypes.add(LogType.NOTE);

        if (isOwner) {
            logTypes.add(LogType.OWNER_MAINTENANCE);
        }
        return logTypes;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    public boolean supportsLogImages() {
        return true;
    }

    @Override
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final Geocache cache) {
        return new SuLoggingManager(activity, this, cache);
    }

    @Override
    public boolean canLog(@NonNull final Geocache cache) {
        return true;
    }

    @Override
    public boolean canAddToWatchList(@NonNull final Geocache cache) {
        return true;
    }

    @Override
    public boolean addToWatchlist(@NonNull final Geocache cache) {
        return SuApi.setWatchState(cache, true);
    }

    @Override
    public boolean removeFromWatchlist(@NonNull final Geocache cache) {
        return SuApi.setWatchState(cache, false);
    }

    /**
     * Whether or not the connector supports adding a note to a specific cache. In most cases the argument will not be
     * relevant.
     *
     * @param cache geocache
     */
    @Override
    public boolean canAddPersonalNote(@NonNull final Geocache cache) {
        return true;
    }

    /**
     * Upload personal note (already stored as member of the cache) to the connector website.
     *
     * @param cache geocache
     * @return success
     */
    @Override
    public boolean uploadPersonalNote(@NonNull final Geocache cache) {
        return SuApi.uploadPersonalNote(cache);
    }

    /**
     * Returns the maximum number of characters allowed in personal notes.
     *
     * @return max number of characters
     */
    @Override
    public int getPersonalNoteMaxChars() {
        return 9500;
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull
        private static final SuConnector INSTANCE = new SuConnector();
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
        return geocode.substring(2);
    }

    /**
     * Returns the web address to create an account
     *
     * @return web address
     */
    @Override
    public String getCreateAccountUrl() {
        return StringUtils.join(getHostUrl(), "/?pn=14");
    }

}
