package cgeo.geocaching.connector.gc;

import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.capability.FieldNotesCapability;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByFinder;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByNextPage;
import cgeo.geocaching.connector.capability.ISearchByOwner;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.capability.IgnoreCapability;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import rx.functions.Action1;

public class GCConnector extends AbstractConnector implements ISearchByGeocode, ISearchByCenter, ISearchByNextPage, ISearchByViewPort, ISearchByKeyword, ILogin, ICredentials, ISearchByOwner, ISearchByFinder, FieldNotesCapability, IgnoreCapability, WatchListCapability {

    @NonNull
    private static final String CACHE_URL_SHORT = "http://coord.info/";
    // Double slash is used to force open in browser
    @NonNull
    private static final String CACHE_URL_LONG = "https://www.geocaching.com/seek/cache_details.aspx?wp=";
    /**
     * Pocket queries downloaded from the website use a numeric prefix. The pocket query creator Android app adds a
     * verbatim "pocketquery" prefix.
     */
    @NonNull
    private static final Pattern GPX_ZIP_FILE_PATTERN = Pattern.compile("((\\d{7,})|(pocketquery))" + "(_.+)?" + "\\.zip", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for GC codes
     */
    @NonNull
    private static final Pattern PATTERN_GC_CODE = Pattern.compile("GC[0-9A-Z&&[^ILOSU]]+", Pattern.CASE_INSENSITIVE);

    private GCConnector() {
        // singleton
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull private static final GCConnector INSTANCE = new GCConnector();
    }

    @NonNull
    public static GCConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return PATTERN_GC_CODE.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getLongCacheUrl(@NonNull final Geocache cache) {
        return CACHE_URL_LONG + cache.getGeocode();
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
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
    public boolean canAddToWatchList(@NonNull final Geocache cache) {
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
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final Geocache cache) {
        return new GCLoggingManager(activity, cache);
    }

    @Override
    public boolean canLog(@NonNull final Geocache cache) {
        return StringUtils.isNotBlank(cache.getCacheId());
    }

    @Override
    @NonNull
    public String getName() {
        return "geocaching.com";
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.geocaching.com";
    }

    @Override
    @NonNull
    public String getTestUrl() {
        return "https://" + getHost() + "/play";
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final CancellableHandler handler) {

        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final String page = GCParser.requestHtmlPage(geocode, guid, "y");

        if (StringUtils.isEmpty(page)) {
            final SearchResult search = new SearchResult();
            if (DataStore.isThere(geocode, guid, false)) {
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
    public SearchResult searchByNextPage(final SearchResult search) {
        return GCParser.searchByNextPage(search);
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport, @Nullable final MapTokens tokens) {
        return GCMap.searchByViewport(viewport, tokens);
    }

    @Override
    public boolean isZippedGPXFile(@NonNull final String fileName) {
        return GPX_ZIP_FILE_PATTERN.matcher(fileName).matches();
    }

    @Override
    public boolean isReliableLatLon(final boolean cacheHasReliableLatLon) {
        return cacheHasReliableLatLon;
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        final String user = Settings.getUserName();
        return StringUtils.isNotEmpty(user) && StringUtils.equalsIgnoreCase(cache.getOwnerUserId(), user);
    }

    @Override
    public boolean addToWatchlist(@NonNull final Geocache cache) {
        final boolean added = GCParser.addToWatchlist(cache);
        if (added) {
            DataStore.saveChangedCache(cache);
        }
        return added;
    }

    @Override
    public boolean removeFromWatchlist(@NonNull final Geocache cache) {
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
     * @return {@code true} if the cache was successfully added, {@code false} otherwise
     */

    public static boolean addToFavorites(final Geocache cache) {
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
     * @return {@code true} if the cache was successfully added, {@code false} otherwise
     */

    public static boolean removeFromFavorites(final Geocache cache) {
        final boolean removed = GCParser.removeFromFavorites(cache);
        if (removed) {
            DataStore.saveChangedCache(cache);
        }
        return removed;
    }

    @Override
    public boolean uploadModifiedCoordinates(@NonNull final Geocache cache, @NonNull final Geopoint wpt) {
        final boolean uploaded = GCParser.uploadModifiedCoordinates(cache, wpt);
        if (uploaded) {
            DataStore.saveChangedCache(cache);
        }
        return uploaded;
    }

    @Override
    public boolean deleteModifiedCoordinates(@NonNull final Geocache cache) {
        final boolean deleted = GCParser.deleteModifiedCoordinates(cache);
        if (deleted) {
            DataStore.saveChangedCache(cache);
        }
        return deleted;
    }

    @Override
    public boolean uploadPersonalNote(@NonNull final Geocache cache) {
        final boolean uploaded = GCParser.uploadPersonalNote(cache);
        if (uploaded) {
            DataStore.saveChangedCache(cache);
        }
        return uploaded;
    }

    @Override
    public SearchResult searchByCenter(@NonNull final Geopoint center) {
        return GCParser.searchByCoords(center, Settings.getCacheType());
    }

    @Override
    public boolean supportsFavoritePoints(@NonNull final Geocache cache) {
        return !cache.getType().isEvent();
    }

    @Override
    public boolean supportsAddToFavorite(final Geocache cache, final LogType type) {
        return cache.supportsFavoritePoints() && Settings.isGCPremiumMember() && !cache.isOwner() && type == LogType.FOUND_IT;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return StringUtils.EMPTY; // UNUSED
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final String noQueryString = StringUtils.substringBefore(url, "?");
        // coord.info URLs
        final String afterCoord = StringUtils.substringAfterLast(noQueryString, "coord.info/");
        if (canHandle(afterCoord)) {
            return afterCoord;
        }
        // expanded geocaching.com URLs
        final String afterGeocache = StringUtils.substringBetween(noQueryString, "/geocache/", "_");
        if (afterGeocache != null && canHandle(afterGeocache)) {
            return afterGeocache;
        }
        return null;
    }

    @Override
    public boolean isActive() {
        return Settings.isGCConnectorActive();
    }

    @Override
    public int getCacheMapMarkerId(final boolean disabled) {
        if (disabled) {
            return R.drawable.marker_disabled;
        }
        return R.drawable.marker;
    }

    @Override
    public boolean login(final Handler handler, @Nullable final Activity fromActivity) {
        // login
        final StatusCode status = GCLogin.getInstance().login();

        if (ConnectorFactory.showLoginToast && handler != null) {
            handler.sendMessage(handler.obtainMessage(0, status));
            ConnectorFactory.showLoginToast = false;

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
    public Credentials getCredentials() {
        return Settings.getCredentials(R.string.pref_username, R.string.pref_password);
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
    public String getWaypointGpxId(final String prefix, @NonNull final String geocode) {
        String gpxId = prefix;
        if (StringUtils.isNotBlank(geocode) && geocode.length() > 2) {
            gpxId += geocode.substring(2);
        }
        return gpxId;
    }

    @Override
    @NonNull
    public String getWaypointPrefix(final String name) {
        String prefix = name;
        if (StringUtils.isNotBlank(prefix) && prefix.length() >= 2) {
            prefix = name.substring(0, 2);
        }
        return prefix;
    }

    @Override
    public SearchResult searchByKeyword(@NonNull final String keyword) {
        return GCParser.searchByKeyword(keyword, Settings.getCacheType());
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
    public int getAvatarPreferenceKey() {
        return R.string.pref_gc_avatar;
    }

    @NonNull
    @Override
    public List<UserAction> getUserActions() {
        final List<UserAction> actions = super.getUserActions();
        actions.add(new UserAction(R.string.user_menu_open_browser, new Action1<UserAction.Context>() {

            @Override
            public void call(final UserAction.Context context) {
                context.activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.geocaching.com/profile/?u=" + Network.encode(context.userName))));
            }
        }));
        actions.add(new UserAction(R.string.user_menu_send_message, new Action1<UserAction.Context>() {

            @Override
            public void call(final UserAction.Context context) {
                try {
                    context.activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.geocaching.com/email/?u=" + Network.encode(context.userName))));
                } catch (final ActivityNotFoundException e) {
                    Log.e("Cannot find suitable activity", e);
                    ActivityMixin.showToast(context.activity, R.string.err_application_no);
                }
            }
        }));
        return actions;
    }

    @Override
    public SearchResult searchByOwner(@NonNull final String username) {
        return GCParser.searchByOwner(username, Settings.getCacheType());
    }

    @Override
    public SearchResult searchByFinder(@NonNull final String username) {
        return GCParser.searchByUsername(username, Settings.getCacheType());
    }

    @Override
    public boolean uploadFieldNotes(@NonNull final File exportFile) {
        if (!GCLogin.getInstance().isActualLoginStatus()) {
            // no need to upload (possibly large file) if we're not logged in
            final StatusCode loginState = GCLogin.getInstance().login();
            if (loginState != StatusCode.NO_ERROR) {
                Log.e("FieldnoteExport.ExportTask upload: Login failed");
                return false;
            }
        }

        final String uri = "https://www.geocaching.com/my/uploadfieldnotes.aspx";
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

    @Override
    public boolean canIgnoreCache(@NonNull final Geocache cache) {
        return StringUtils.isNotEmpty(cache.getType().wptTypeId) && Settings.isGCPremiumMember();
    }

    @Override
    public void ignoreCache(@NonNull final Geocache cache) {
        GCParser.ignoreCache(cache);
    }

    @Override
    @Nullable
    public String getCreateAccountUrl() {
        return "https://www.geocaching.com/account/register";
    }
}
