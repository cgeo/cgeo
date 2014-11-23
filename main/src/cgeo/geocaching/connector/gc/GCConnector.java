package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.capability.FieldNotesCapability;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByFinder;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByOwner;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.functions.Action1;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class GCConnector extends AbstractConnector implements ISearchByGeocode, ISearchByCenter, ISearchByViewPort, ISearchByKeyword, ILogin, ICredentials, ISearchByOwner, ISearchByFinder, FieldNotesCapability {

    private static final String CACHE_URL_SHORT = "http://coord.info/";
    // Double slash is used to force open in browser
    private static final String CACHE_URL_LONG = "http://www.geocaching.com/seek/cache_details.aspx?wp=";
    /**
     * Pocket queries downloaded from the website use a numeric prefix. The pocket query creator Android app adds a
     * verbatim "pocketquery" prefix.
     */
    private static final Pattern GPX_ZIP_FILE_PATTERN = Pattern.compile("((\\d{7,})|(pocketquery))" + "(_.+)?" + "\\.zip", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for GC codes
     */
    private final static Pattern PATTERN_GC_CODE = Pattern.compile("GC[0-9A-Z]+", Pattern.CASE_INSENSITIVE);

    private GCConnector() {
        // singleton
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static final @NonNull GCConnector INSTANCE = new GCConnector();
    }

    public static @NonNull
    GCConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean canHandle(@NonNull String geocode) {
        return GCConnector.PATTERN_GC_CODE.matcher(geocode).matches();
    }

    @Override
    public String getLongCacheUrl(@NonNull Geocache cache) {
        return CACHE_URL_LONG + cache.getGeocode();
    }

    @Override
    public String getCacheUrl(@NonNull Geocache cache) {
        return CACHE_URL_SHORT + cache.getGeocode();
    }

    @Override
    public boolean supportsPersonalNote() {
        return Settings.isGCPremiumMember();
    }

    @Override
    public boolean supportsOwnCoordinates() {
        return true;
    }

    @Override
    public boolean supportsWatchList() {
        return true;
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
    public ILoggingManager getLoggingManager(final LogCacheActivity activity, final Geocache cache) {
        return new GCLoggingManager(activity, cache);
    }

    @Override
    public boolean canLog(Geocache cache) {
        return StringUtils.isNotBlank(cache.getCacheId());
    }

    @Override
    public String getName() {
        return "geocaching.com";
    }

    @Override
    public String getHost() {
        return "www.geocaching.com";
    }

    @Override
    public SearchResult searchByGeocode(final @Nullable String geocode, final @Nullable String guid, final CancellableHandler handler) {

        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final String page = GCParser.requestHtmlPage(geocode, guid, "y");

        if (StringUtils.isEmpty(page)) {
            final SearchResult search = new SearchResult();
            if (DataStore.isThere(geocode, guid, true, false)) {
                if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                    Log.i("Loading old cache from cache.");
                    search.addGeocode(DataStore.getGeocodeForGuid(guid));
                } else {
                    search.addGeocode(geocode);
                }
                search.setError(StatusCode.NO_ERROR);
                return search;
            }

            Log.e("GCConnector.searchByGeocode: No data from server");
            search.setError(StatusCode.COMMUNICATION_ERROR);
            return search;
        }
        assert page != null;

        final SearchResult searchResult = GCParser.parseCache(page, handler);

        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCConnector.searchByGeocode: No cache parsed");
            return searchResult;
        }

        // do not filter when searching for one specific cache
        return searchResult;
    }

    @Override
    public SearchResult searchByViewport(@NonNull Viewport viewport, final MapTokens tokens) {
        return GCMap.searchByViewport(viewport, tokens);
    }

    @Override
    public boolean isZippedGPXFile(final String fileName) {
        return GPX_ZIP_FILE_PATTERN.matcher(fileName).matches();
    }

    @Override
    public boolean isReliableLatLon(boolean cacheHasReliableLatLon) {
        return cacheHasReliableLatLon;
    }

    @Override
    public boolean isOwner(final Geocache cache) {
        final String user = Settings.getUsername();
        return StringUtils.isNotEmpty(user) && StringUtils.equalsIgnoreCase(cache.getOwnerUserId(), user);
    }

    @Override
    public boolean addToWatchlist(Geocache cache) {
        final boolean added = GCParser.addToWatchlist(cache);
        if (added) {
            DataStore.saveChangedCache(cache);
        }
        return added;
    }

    @Override
    public boolean removeFromWatchlist(Geocache cache) {
        final boolean removed = GCParser.removeFromWatchlist(cache);
        if (removed) {
            DataStore.saveChangedCache(cache);
        }
        return removed;
    }

    /**
     * Add a cache to the favorites list.
     *
     * This must not be called from the UI thread.
     *
     * @param cache
     *            the cache to add
     * @return <code>true</code> if the cache was successfully added, <code>false</code> otherwise
     */

    public static boolean addToFavorites(Geocache cache) {
        final boolean added = GCParser.addToFavorites(cache);
        if (added) {
            DataStore.saveChangedCache(cache);
        }
        return added;
    }

    /**
     * Remove a cache from the favorites list.
     *
     * This must not be called from the UI thread.
     *
     * @param cache
     *            the cache to add
     * @return <code>true</code> if the cache was successfully added, <code>false</code> otherwise
     */

    public static boolean removeFromFavorites(Geocache cache) {
        final boolean removed = GCParser.removeFromFavorites(cache);
        if (removed) {
            DataStore.saveChangedCache(cache);
        }
        return removed;
    }

    @Override
    public boolean uploadModifiedCoordinates(Geocache cache, Geopoint wpt) {
        final boolean uploaded = GCParser.uploadModifiedCoordinates(cache, wpt);
        if (uploaded) {
            DataStore.saveChangedCache(cache);
        }
        return uploaded;
    }

    @Override
    public boolean deleteModifiedCoordinates(Geocache cache) {
        final boolean deleted = GCParser.deleteModifiedCoordinates(cache);
        if (deleted) {
            DataStore.saveChangedCache(cache);
        }
        return deleted;
    }

    @Override
    public boolean uploadPersonalNote(Geocache cache) {
        final boolean uploaded = GCParser.uploadPersonalNote(cache);
        if (uploaded) {
            DataStore.saveChangedCache(cache);
        }
        return uploaded;
    }

    @Override
    public SearchResult searchByCenter(@NonNull Geopoint center, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return GCParser.searchByCoords(center, Settings.getCacheType(), Settings.isShowCaptcha(), recaptchaReceiver);
    }

    @Override
    public boolean supportsFavoritePoints(final Geocache cache) {
        return !cache.getType().isEvent();
    }

    @Override
    protected String getCacheUrlPrefix() {
        return null; // UNUSED
    }

    @Override
    public String getGeocodeFromUrl(String url) {
        // coord.info URLs
        String code = StringUtils.substringAfterLast(url, "coord.info/");
        if (code != null && canHandle(code)) {
            return code;
        }
        // expanded geocaching.com URLs
        code = StringUtils.substringBetween(url, "/geocache/", "_");
        if (code != null && canHandle(code)) {
            return code;
        }
        return null;
    }

    @Override
    public boolean isActive() {
        return Settings.isGCConnectorActive();
    }

    @Override
    public int getCacheMapMarkerId(boolean disabled) {
        if (disabled) {
            return R.drawable.marker_disabled;
        }
        return R.drawable.marker;
    }

    @Override
    public boolean login(Handler handler, Context fromActivity) {
        // login
        final StatusCode status = GCLogin.getInstance().login();

        if (CgeoApplication.getInstance().showLoginToast && handler != null) {
            handler.sendMessage(handler.obtainMessage(0, status));
            CgeoApplication.getInstance().showLoginToast = false;

            // invoke settings activity to insert login details
            if (status == StatusCode.NO_LOGIN_INFO_STORED && fromActivity != null) {
                SettingsActivity.openForScreen(R.string.preference_screen_gc, fromActivity);
            }
        }
        return status == StatusCode.NO_ERROR;
    }

    @Override
    public void logout() {
        GCLogin.getInstance().logout();
    }

    @Override
    public String getUserName() {
        return GCLogin.getInstance().getActualUserName();
    }

    @Override
    public int getCachesFound() {
        return GCLogin.getInstance().getActualCachesFound();
    }

    @Override
    public String getLoginStatusString() {
        return GCLogin.getInstance().getActualStatus();
    }

    @Override
    public boolean isLoggedIn() {
        return GCLogin.getInstance().isActualLoginStatus();
    }

    @Override
    public String getWaypointGpxId(String prefix, String geocode) {
        String gpxId = prefix;
        if (StringUtils.isNotBlank(geocode) && geocode.length() > 2) {
            gpxId += geocode.substring(2);
        }
        return gpxId;
    }

    @Override
    public String getWaypointPrefix(String name) {
        String prefix = name;
        if (StringUtils.isNotBlank(prefix) && prefix.length() >= 2) {
            prefix = name.substring(0, 2);
        }
        return prefix;
    }

    @Override
    public SearchResult searchByKeyword(@NonNull String keyword, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return GCParser.searchByKeyword(keyword, Settings.getCacheType(), Settings.isShowCaptcha(), recaptchaReceiver);
    }

    @Override
    public int getUsernamePreferenceKey() {
        return R.string.pref_username;
    }

    @Override
    public int getPasswordPreferenceKey() {
        return R.string.pref_password;
    }

    @Override
    public @NonNull
    List<UserAction> getUserActions() {
        List<UserAction> actions = super.getUserActions();
        actions.add(new UserAction(R.string.user_menu_open_browser, new Action1<UserAction.Context>() {

            @Override
            public void call(cgeo.geocaching.connector.UserAction.Context context) {
                context.activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/profile/?u=" + Network.encode(context.userName))));
            }
        }));
        actions.add(new UserAction(R.string.user_menu_send_message, new Action1<UserAction.Context>() {

            @Override
            public void call(cgeo.geocaching.connector.UserAction.Context context) {
                try {
                    context.activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/email/?u=" + Network.encode(context.userName))));
                } catch (final ActivityNotFoundException e) {
                    Log.e("Cannot find suitable activity", e);
                    ActivityMixin.showToast(context.activity, R.string.err_application_no);
                }
            }
        }));
        return actions;
    }

    @Override
    public SearchResult searchByOwner(final @NonNull String username, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return GCParser.searchByOwner(username, Settings.getCacheType(), Settings.isShowCaptcha(), recaptchaReceiver);
    }

    @Override
    public SearchResult searchByFinder(final @NonNull String username, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return GCParser.searchByUsername(username, Settings.getCacheType(), Settings.isShowCaptcha(), recaptchaReceiver);
    }

    @Override
    public boolean uploadFieldNotes(final File exportFile) {
        if (!GCLogin.getInstance().isActualLoginStatus()) {
            // no need to upload (possibly large file) if we're not logged in
            final StatusCode loginState = GCLogin.getInstance().login();
            if (loginState != StatusCode.NO_ERROR) {
                Log.e("FieldnoteExport.ExportTask upload: Login failed");
            }
        }

        final String uri = "http://www.geocaching.com/my/uploadfieldnotes.aspx";
        final String page = GCLogin.getInstance().getRequestLogged(uri, null);

        if (StringUtils.isBlank(page)) {
            Log.e("FieldnoteExport.ExportTask get page: No data from server");
            return false;
        }

        final String[] viewstates = GCLogin.getViewstates(page);

        final Parameters uploadParams = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "ctl00$ContentBody$btnUpload", "Upload Field Note");

        GCLogin.putViewstates(uploadParams, viewstates);

        Network.getResponseData(Network.postRequest(uri, uploadParams, "ctl00$ContentBody$FieldNoteLoader", "text/plain", exportFile));

        if (StringUtils.isBlank(page)) {
            Log.e("FieldnoteExport.ExportTask upload: No data from server");
            return false;
        }
        return true;
    }

}
