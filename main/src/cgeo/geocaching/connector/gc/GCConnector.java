package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import java.util.regex.Pattern;

public class GCConnector extends AbstractConnector implements ISearchByGeocode, ISearchByCenter, ISearchByViewPort, ILogin {

    private static final String CACHE_URL_SHORT = "http://coord.info/";
    // Double slash is used to force open in browser
    private static final String CACHE_URL_LONG = "http://www.geocaching.com//seek/cache_details.aspx?wp=";
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
        private static final GCConnector INSTANCE = new GCConnector();
    }

    public static GCConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean canHandle(String geocode) {
        if (geocode == null) {
            return false;
        }
        return GCConnector.PATTERN_GC_CODE.matcher(geocode).matches();
    }

    @Override
    public String getLongCacheUrl(Geocache cache) {
        return CACHE_URL_LONG + cache.getGeocode();
    }

    @Override
    public String getCacheUrl(Geocache cache) {
        return CACHE_URL_SHORT + cache.getGeocode();
    }

    @Override
    public boolean supportsPersonalNote() {
        return true;
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
    public ILoggingManager getLoggingManager(Activity activity, Geocache cache) {
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
    public boolean supportsUserActions() {
        return true;
    }

    @Override
    public SearchResult searchByGeocode(final String geocode, final String guid, final CancellableHandler handler) {

        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final String page = GCParser.requestHtmlPage(geocode, guid, "y", String.valueOf(GCConstants.NUMBER_OF_LOGS));

        if (StringUtils.isEmpty(page)) {
            final SearchResult search = new SearchResult();
            if (cgData.isThere(geocode, guid, true, false)) {
                if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                    Log.i("Loading old cache from cache.");
                    search.addGeocode(cgData.getGeocodeForGuid(guid));
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

        final SearchResult searchResult = GCParser.parseCache(page, handler);

        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCConnector.searchByGeocode: No cache parsed");
            return searchResult;
        }

        // do not filter when searching for one specific cache
        return searchResult;
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String[] tokens) {
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
    public boolean isOwner(final ICache cache) {
        return StringUtils.equalsIgnoreCase(cache.getOwnerUserId(), Settings.getUsername());

    }

    @Override
    public boolean addToWatchlist(Geocache cache) {
        final boolean added = GCParser.addToWatchlist(cache);
        if (added) {
            cgData.saveChangedCache(cache);
        }
        return added;
    }

    @Override
    public boolean removeFromWatchlist(Geocache cache) {
        final boolean removed = GCParser.removeFromWatchlist(cache);
        if (removed) {
            cgData.saveChangedCache(cache);
        }
        return removed;
    }

    /**
     * Add a cache to the favorites list.
     *
     * This must not be called from the UI thread.
     *
     * @param cache the cache to add
     * @return <code>true</code> if the cache was sucessfully added, <code>false</code> otherwise
     */

    public static boolean addToFavorites(Geocache cache) {
        final boolean added = GCParser.addToFavorites(cache);
        if (added) {
            cgData.saveChangedCache(cache);
        }
        return added;
    }

    /**
     * Remove a cache from the favorites list.
     *
     * This must not be called from the UI thread.
     *
     * @param cache the cache to add
     * @return <code>true</code> if the cache was sucessfully added, <code>false</code> otherwise
     */

    public static boolean removeFromFavorites(Geocache cache) {
        final boolean removed = GCParser.removeFromFavorites(cache);
        if (removed) {
            cgData.saveChangedCache(cache);
        }
        return removed;
    }

    @Override
    public boolean uploadModifiedCoordinates(Geocache cache, Geopoint wpt) {
        final boolean uploaded = GCParser.uploadModifiedCoordinates(cache, wpt);
        if (uploaded) {
            cgData.saveChangedCache(cache);
        }
        return uploaded;
    }

    @Override
    public boolean deleteModifiedCoordinates(Geocache cache) {
        final boolean deleted = GCParser.deleteModifiedCoordinates(cache);
        if (deleted) {
            cgData.saveChangedCache(cache);
        }
        return deleted;
    }

    @Override
    public boolean uploadPersonalNote(Geocache cache) {
        final boolean uploaded = GCParser.uploadPersonalNote(cache);
        if (uploaded) {
            cgData.saveChangedCache(cache);
        }
        return uploaded;
    }

    @Override
    public SearchResult searchByCenter(Geopoint center) {
        // TODO make search by coordinate use this method. currently it is just a marker that this connector supports search by center
        return null;
    }

    @Override
    public boolean supportsFavoritePoints() {
        return true;
    }

    @Override
    protected String getCacheUrlPrefix() {
        return CACHE_URL_SHORT;
    }

    @Override
    public boolean isActivated() {
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
        final StatusCode status = Login.login();

        if (status == StatusCode.NO_ERROR) {
            cgeoapplication.getInstance().checkLogin = false;
            Login.detectGcCustomDate();
        }

        if (cgeoapplication.getInstance().showLoginToast && handler != null) {
            handler.sendMessage(handler.obtainMessage(0, status));
            cgeoapplication.getInstance().showLoginToast = false;

            // invoke settings activity to insert login details
            if (status == StatusCode.NO_LOGIN_INFO_STORED && fromActivity != null) {
                SettingsActivity.jumpToServicesPage(fromActivity);
            }
        }
        return status == StatusCode.NO_ERROR;
    }

    @Override
    public String getUserName() {
        return Login.getActualUserName();
    }

    @Override
    public int getCachesFound() {
        return Login.getActualCachesFound();
    }

    @Override
    public String getLoginStatusString() {
        return Login.getActualStatus();
    }

    @Override
    public boolean isLoggedIn() {
        return Login.isActualLoginStatus();
    }
}
