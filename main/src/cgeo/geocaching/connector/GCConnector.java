package cgeo.geocaching.connector;

import cgeo.geocaching.Parameters;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.util.Log;

import java.util.List;
import java.util.regex.Pattern;

public class GCConnector extends AbstractConnector {

    private static GCConnector instance;
    private static final Pattern gpxZipFilePattern = Pattern.compile("\\d{7,}(_.+)?\\.zip", Pattern.CASE_INSENSITIVE);

    private GCConnector() {
        // singleton
    }

    public static GCConnector getInstance() {
        if (instance == null) {
            instance = new GCConnector();
        }
        return instance;
    }

    @Override
    public boolean canHandle(String geocode) {
        return StringUtils.startsWithIgnoreCase(geocode, "GC");
    }

    @Override
    public boolean supportsRefreshCache(cgCache cache) {
        return true;
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        // it would also be possible to use "http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.getGeocode();
        return "http://coord.info/" + cache.getGeocode();
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
    public String getName() {
        return "GeoCaching.com";
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
    public boolean supportsCachesAround() {
        return true;
    }

    @Override
    public cgSearch searchByGeocode(final String geocode, final String guid, final cgeoapplication app, final cgSearch search, final int listId, final CancellableHandler handler) {
        final Parameters params = new Parameters("decrypt", "y");
        if (StringUtils.isNotBlank(geocode)) {
            params.put("wp", geocode);
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid);
        }

        if (app == null) {
            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No application found");
            return null;
        }

        cgBase.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final String page = cgBase.requestLogged("http://www.geocaching.com/seek/cache_details.aspx", params, false, false, false);

        if (StringUtils.isEmpty(page)) {
            if (app.isThere(geocode, guid, true, false)) {
                if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                    Log.i(Settings.tag, "Loading old cache from cache.");

                    search.addGeocode(app.getGeocode(guid));
                } else {
                    search.addGeocode(geocode);
                }
                search.error = null;
                return search;
            }

            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No data from server");
            return null;
        }

        final cgCacheWrap caches = cgBase.parseCache(page, listId, handler);

        if (caches == null || CollectionUtils.isEmpty(caches.cacheList)) {
            if (caches != null && caches.error != null) {
                search.error = caches.error;
            }
            if (caches != null && StringUtils.isNotBlank(caches.url)) {
                search.url = caches.url;
            }

            app.addSearch(null, listId);

            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No cache parsed");
            return search;
        }

        final List<cgCache> cacheList = cgBase.filterSearchResults(search, caches, false, false, Settings.getCacheType());
        app.addSearch(cacheList, listId);

        return search;
    }

    @Override
    public boolean isZippedGPXFile(final String fileName) {
        return gpxZipFilePattern.matcher(fileName).matches();
    }
}
