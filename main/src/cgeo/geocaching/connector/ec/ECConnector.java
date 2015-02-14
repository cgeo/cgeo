package cgeo.geocaching.connector.ec;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class ECConnector extends AbstractConnector implements ISearchByGeocode, ISearchByCenter, ISearchByViewPort, ILogin, ICredentials {

    @NonNull
    private static final String CACHE_URL = "http://extremcaching.com/index.php/output-2/";

    /**
     * Pattern for EC codes
     */
    @NonNull
    private final static Pattern PATTERN_EC_CODE = Pattern.compile("EC[0-9]+", Pattern.CASE_INSENSITIVE);

    private final CgeoApplication app = CgeoApplication.getInstance();

    @NonNull
    private final ECLogin ecLogin = ECLogin.getInstance();

    private ECConnector() {
        // singleton
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static final @NonNull ECConnector INSTANCE = new ECConnector();
    }

    @NonNull
    public static ECConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return ECConnector.PATTERN_EC_CODE.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return CACHE_URL + cache.getGeocode().replace("EC", "");
    }

    @Override
    @NonNull
    public String getName() {
        return "extremcaching.com";
    }

    @Override
    @NonNull
    public String getHost() {
        return "extremcaching.com";
    }

    @Override
    public SearchResult searchByGeocode(final @Nullable String geocode, final @Nullable String guid, final CancellableHandler handler) {
        if (geocode == null) {
            return null;
        }
        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final Geocache cache = ECApi.searchByGeoCode(geocode);

        return cache != null ? new SearchResult(cache) : null;
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport, @NonNull final MapTokens tokens) {
        final Collection<Geocache> caches = ECApi.searchByBBox(viewport);
        final SearchResult searchResult = new SearchResult(caches);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    @NonNull
    public SearchResult searchByCenter(@NonNull final Geopoint center, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        final Collection<Geocache> caches = ECApi.searchByCenter(center);
        final SearchResult searchResult = new SearchResult(caches);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return CACHE_URL;
    }

    @Override
    public boolean isActive() {
        return Settings.isECConnectorActive();
    }

    @Override
    public boolean login(final Handler handler, final Context fromActivity) {
        // login
        final StatusCode status = ecLogin.login();

        if (app.showLoginToast && handler != null) {
            handler.sendMessage(handler.obtainMessage(0, status));
            app.showLoginToast = false;

            // invoke settings activity to insert login details
            if (status == StatusCode.NO_LOGIN_INFO_STORED && fromActivity != null) {
                SettingsActivity.openForScreen(R.string.preference_screen_ec, fromActivity);
            }
        }
        return status == StatusCode.NO_ERROR;
    }

    @Override
    public String getUserName() {
        return ecLogin.getActualUserName();
    }

    @Override
    public int getCachesFound() {
        return ecLogin.getActualCachesFound();
    }

    @Override
    public String getLoginStatusString() {
        return ecLogin.getActualStatus();
    }

    @Override
    public boolean isLoggedIn() {
        return ecLogin.isActualLoginStatus();
    }

    @Override
    public int getCacheMapMarkerId(final boolean disabled) {
        final String icons = Settings.getECIconSet();
        if (StringUtils.equals(icons, "1")) {
            return disabled ? R.drawable.marker_disabled_other : R.drawable.marker_other;
        }
        return disabled ? R.drawable.marker_disabled_oc : R.drawable.marker_oc;
    }

    @Override
    @NonNull
    public String getLicenseText(final @NonNull Geocache cache) {
        // NOT TO BE TRANSLATED
        return "© " + cache.getOwnerDisplayName() + ", <a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a>, CC BY-NC-ND 3.0, alle Logeinträge © jeweiliger Autor";
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    public boolean canLog(@NonNull final Geocache cache) {
        return true;
    }

    @Override
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final Geocache cache) {
        return new ECLoggingManager(activity, this, cache);
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes(@NonNull final Geocache geocache) {
        final List<LogType> logTypes = new ArrayList<>();
        if (geocache.isEventCache()) {
            logTypes.add(LogType.WILL_ATTEND);
            logTypes.add(LogType.ATTENDED);
        } else {
            logTypes.add(LogType.FOUND_IT);
        }
        if (!geocache.isEventCache()) {
            logTypes.add(LogType.DIDNT_FIND_IT);
        }
        logTypes.add(LogType.NOTE);
        return logTypes;
    }

    @Override
    public int getMaxTerrain() {
        return 7;
    }

    @Override
    public int getUsernamePreferenceKey() {
        return R.string.pref_ecusername;
    }

    @Override
    public int getPasswordPreferenceKey() {
        return R.string.pref_ecpassword;
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final String geocode = "EC" + StringUtils.substringAfter(url, "extremcaching.com/index.php/output-2/");
        if (canHandle(geocode)) {
            return geocode;
        }
        return super.getGeocodeFromUrl(url);
    }

}
