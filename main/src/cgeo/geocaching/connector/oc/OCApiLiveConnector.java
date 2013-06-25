package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.oc.OkapiClient.UserInfo;
import cgeo.geocaching.connector.oc.OkapiClient.UserInfo.UserInfoStatus;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

public class OCApiLiveConnector extends OCApiConnector implements ISearchByCenter, ISearchByViewPort, ILogin {

    private String cS;
    private UserInfo userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfoStatus.NOT_RETRIEVED);

    public OCApiLiveConnector(String name, String host, String prefix, int cKResId, int cSResId, ApiSupport apiSupport) {
        super(name, host, prefix, CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cKResId)), apiSupport);

        cS = CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cSResId));
    }

    @Override
    public boolean isActivated() {
        return Settings.isOCConnectorActive();
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
        // TODO the tokens must be available connector specific
        if (StringUtils.isNotBlank(Settings.getOCDETokenPublic()) && StringUtils.isNotBlank(Settings.getOCDETokenSecret())) {
            return OAuthLevel.Level3;
        }
        return OAuthLevel.Level1;
    }

    @Override
    public String getCS() {
        return CryptUtils.rot13(cS);
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
        switch (userInfo.getStatus()) {
            case NOT_RETRIEVED:
                return cgeoapplication.getInstance().getString(R.string.init_login_popup_working);
            case SUCCESSFUL:
                return cgeoapplication.getInstance().getString(R.string.init_login_popup_ok);
            case FAILED:
                return cgeoapplication.getInstance().getString(R.string.init_login_popup_failed);
            case NOT_SUPPORTED:
                return cgeoapplication.getInstance().getString(R.string.init_login_popup_not_authorized);
            default:
                return "Error";
        }
    }

    @Override
    public boolean isLoggedIn() {
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL;
    }
}
