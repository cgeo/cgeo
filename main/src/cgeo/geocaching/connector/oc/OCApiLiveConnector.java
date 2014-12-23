package cgeo.geocaching.connector.oc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByFinder;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByOwner;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.connector.oc.UserInfo.UserInfoStatus;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;
import android.os.Handler;

public class OCApiLiveConnector extends OCApiConnector implements ISearchByCenter, ISearchByViewPort, ILogin, ISearchByKeyword, ISearchByOwner, ISearchByFinder {

    private final String cS;
    private final int isActivePrefKeyId;
    private final int tokenPublicPrefKeyId;
    private final int tokenSecretPrefKeyId;
    private UserInfo userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfoStatus.NOT_RETRIEVED);

    public OCApiLiveConnector(final String name, final String host, final String prefix, final String licenseString, final int cKResId, final int cSResId, final int isActivePrefKeyId, final int tokenPublicPrefKeyId, final int tokenSecretPrefKeyId, final ApiSupport apiSupport) {
        super(name, host, prefix, CryptUtils.rot13(CgeoApplication.getInstance().getResources().getString(cKResId)), licenseString, apiSupport);

        cS = CryptUtils.rot13(CgeoApplication.getInstance().getResources().getString(cSResId));
        this.isActivePrefKeyId = isActivePrefKeyId;
        this.tokenPublicPrefKeyId = tokenPublicPrefKeyId;
        this.tokenSecretPrefKeyId = tokenSecretPrefKeyId;
    }

    @Override
    public boolean isActive() {
        return Settings.isOCConnectorActive(isActivePrefKeyId);
    }

    @Override
    public SearchResult searchByViewport(@NonNull final Viewport viewport, final MapTokens tokens) {
        return new SearchResult(OkapiClient.getCachesBBox(viewport, this));
    }

    @Override
    public SearchResult searchByCenter(@NonNull final Geopoint center, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return new SearchResult(OkapiClient.getCachesAround(center, this));
    }

    @Override
    public SearchResult searchByOwner(@NonNull final String username, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return new SearchResult(OkapiClient.getCachesByOwner(username, this));
    }

    @Override
    public SearchResult searchByFinder(@NonNull final String username, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return new SearchResult(OkapiClient.getCachesByFinder(username, this));
    }

    @Override
    public OAuthLevel getSupportedAuthLevel() {

        if (Settings.hasOCAuthorization(tokenPublicPrefKeyId, tokenSecretPrefKeyId)) {
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
    public boolean supportsWatchList() {
        return ApiSupport.current == getApiSupport();
    }

    @Override
    public boolean addToWatchlist(final Geocache cache) {
        final boolean added = OkapiClient.setWatchState(cache, true, this);

        if (added) {
            DataStore.saveChangedCache(cache);
        }

        return added;
    }

    @Override
    public boolean removeFromWatchlist(final Geocache cache) {
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
    public ILoggingManager getLoggingManager(final LogCacheActivity activity, final Geocache cache) {
        return new OkapiLoggingManager(activity, this, cache);
    }

    @Override
    public boolean canLog(final Geocache cache) {
        return true;
    }

    public boolean supportsPersonalization() {
        return getSupportedAuthLevel() == OAuthLevel.Level3;
    }

    @Override
    public boolean login(final Handler handler, final Context fromActivity) {
        if (supportsPersonalization()) {
            userInfo = OkapiClient.getUserInfo(this);
        } else {
            userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfoStatus.NOT_SUPPORTED);
        }
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL;
    }

    @Override
    public boolean isOwner(final Geocache cache) {
        return StringUtils.isNotEmpty(getUserName()) && StringUtils.equals(cache.getOwnerDisplayName(), getUserName());
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
    public SearchResult searchByKeyword(final @NonNull String name, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        final Geopoint currentPos = CgeoApplication.getInstance().currentGeo().getCoords();
        return new SearchResult(OkapiClient.getCachesNamed(currentPos, name, this));
    }

    @Override
    public boolean isSearchForMyCaches(final String username) {
        return StringUtils.equalsIgnoreCase(username, getUserName());
    }

}
