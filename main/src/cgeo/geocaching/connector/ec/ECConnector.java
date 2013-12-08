package cgeo.geocaching.connector.ec;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.os.Handler;

import java.util.Collection;
import java.util.regex.Pattern;

public class ECConnector extends AbstractConnector implements ISearchByGeocode, ISearchByCenter, ISearchByViewPort, ILogin {

    private static final String CACHE_URL = "http://extremcaching.com/index.php/output-2/";

    /**
     * Pattern for EC codes
     */
    private final static Pattern PATTERN_EC_CODE = Pattern.compile("EC[0-9]+", Pattern.CASE_INSENSITIVE);


    private ECConnector() {
        // singleton
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static final ECConnector INSTANCE = new ECConnector();
    }

    public static ECConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean canHandle(String geocode) {
        if (geocode == null) {
            return false;
        }
        return ECConnector.PATTERN_EC_CODE.matcher(geocode).matches();
    }

    @Override
    public String getCacheUrl(Geocache cache) {
        return CACHE_URL + cache.getGeocode().replace("EC", "");
    }

    @Override
    public String getName() {
        return "extremcaching.com";
    }

    @Override
    public String getHost() {
        return "extremcaching.com";
    }

    @Override
    public SearchResult searchByGeocode(final String geocode, final String guid, final CancellableHandler handler) {

        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final Geocache cache = ECApi.searchByGeoCode(geocode);
        if (cache == null) {
            return null;
        }
        final SearchResult searchResult = new SearchResult(cache);

        return searchResult;
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String[] tokens) {
        final Collection<Geocache> caches = ECApi.searchByBBox(viewport);
        if (caches == null) {
            return null;
        }
        final SearchResult searchResult = new SearchResult(caches);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    public SearchResult searchByCenter(Geopoint center) {
        final Collection<Geocache> caches = ECApi.searchByCenter(center);
        if (caches == null) {
            return null;
        }
        final SearchResult searchResult = new SearchResult(caches);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    public boolean isOwner(final ICache cache) {
        //return StringUtils.equalsIgnoreCase(cache.getOwnerUserId(), Settings.getUsername());
        return false;
    }

    @Override
    protected String getCacheUrlPrefix() {
        return CACHE_URL;
    }

    @Override
    public boolean isActivated() {
        return Settings.isECConnectorActive();
    }

    @Override
    public boolean login(Handler handler, Context fromActivity) {
        // login
        final StatusCode status = ECLogin.login();

        if (status == StatusCode.NO_ERROR) {
            CgeoApplication.getInstance().checkLogin = false;
        }

        if (CgeoApplication.getInstance().showLoginToast && handler != null) {
            handler.sendMessage(handler.obtainMessage(0, status));
            CgeoApplication.getInstance().showLoginToast = false;

            // invoke settings activity to insert login details
            if (status == StatusCode.NO_LOGIN_INFO_STORED && fromActivity != null) {
                SettingsActivity.jumpToServicesPage(fromActivity);
            }
        }
        return status == StatusCode.NO_ERROR;
    }

    @Override
    public String getUserName() {
        return ECLogin.getActualUserName();
    }

    @Override
    public int getCachesFound() {
        return ECLogin.getActualCachesFound();
    }

    @Override
    public String getLoginStatusString() {
        return ECLogin.getActualStatus();
    }

    @Override
    public boolean isLoggedIn() {
        return ECLogin.isActualLoginStatus();
    }

    @Override
    public int getCacheMapMarkerId(boolean disabled) {
        final String icons = Settings.getECIconSet();
        if (StringUtils.equals(icons, "1")) {
            if (disabled) {
                return R.drawable.marker_disabled_other;
            }
            return R.drawable.marker_other;
        }

        if (disabled) {
            return R.drawable.marker_disabled_oc;
        }
        return R.drawable.marker_oc;
    }

    @Override
    public String getLicenseText(final Geocache cache) {
        // NOT TO BE TRANSLATED
        return "© " + cache.getOwnerDisplayName() + ", <a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a>, CC BY-NC-ND 3.0, alle Logeinträge © jeweiliger Autor";
    }

}
