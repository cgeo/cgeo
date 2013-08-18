package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.oc.UserInfo.UserInfoStatus;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

public class OCApiLiveConnector extends OCApiConnector implements ISearchByCenter, ISearchByViewPort, ILogin {

    private final String cS;
    private final int isActivePrefKeyId;
    private final int tokenPublicPrefKeyId;
    private final int tokenSecretPrefKeyId;
    private UserInfo userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfoStatus.NOT_RETRIEVED);

    public OCApiLiveConnector(String name, String host, String prefix, String licenseString, int cKResId, int cSResId, int isActivePrefKeyId, int tokenPublicPrefKeyId, int tokenSecretPrefKeyId, ApiSupport apiSupport) {
        super(name, host, prefix, CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cKResId)), licenseString, apiSupport);

        cS = CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cSResId));
        this.isActivePrefKeyId = isActivePrefKeyId;
        this.tokenPublicPrefKeyId = tokenPublicPrefKeyId;
        this.tokenSecretPrefKeyId = tokenSecretPrefKeyId;
    }

    @Override
    public boolean isActivated() {
        return Settings.isOCConnectorActive(isActivePrefKeyId);
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String[] tokens) {
        return new SearchResult(OkapiClient.getCachesBBox(viewport, this));
    }

    @Override
    public SearchResult searchByCenter(Geopoint center) {

        return new SearchResult(OkapiClient.getCachesAround(center, this));
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
        return true;
    }

    @Override
    public boolean addToWatchlist(Geocache cache) {
        final boolean added = OkapiClient.setWatchState(cache, true, this);

        if (added) {
            cgData.saveChangedCache(cache);
        }

        return added;
    }

    @Override
    public boolean removeFromWatchlist(Geocache cache) {
        final boolean removed = OkapiClient.setWatchState(cache, false, this);

        if (removed) {
            cgData.saveChangedCache(cache);
        }

        return removed;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    public ILoggingManager getLoggingManager(Activity activity, Geocache cache) {
        return new OkapiLoggingManager(activity, this, cache);
    }

    @Override
    public boolean canLog(Geocache cache) {
        return true;
    }

    public boolean supportsPersonalization() {
        return getSupportedAuthLevel() == OAuthLevel.Level3;
    }

    @Override
    public boolean login(Handler handler, Context fromActivity) {
        if (supportsPersonalization()) {
            userInfo = OkapiClient.getUserInfo(this);
        } else {
            userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfoStatus.NOT_SUPPORTED);
        }
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL;
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
        return cgeoapplication.getInstance().getString(userInfo.getStatus().resId);
    }

    @Override
    public boolean isLoggedIn() {
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL;
    }
}
