package cgeo.geocaching.connector;

import cgeo.geocaching.Parameters;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;

import org.apache.commons.lang3.StringUtils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GCConnector extends AbstractConnector implements IConnector {

    @Override
    public boolean canHandle(String geocode) {
        return StringUtils.isNotBlank(geocode) && StringUtils.startsWithIgnoreCase(geocode, "GC");
    }

    @Override
    public boolean supportsRefreshCache(cgCache cache) {
        return true;
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        // it would also be possible to use "http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.geocode;
        return "http://coord.info/" + cache.geocode;
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
    public UUID searchByGeocode(final cgBase base, String geocode, final String guid, final cgeoapplication app, final cgSearch search, final int reason) {
        final Parameters params = new Parameters("decrypt", "y");
        if (StringUtils.isNotBlank(geocode)) {
            params.put("wp", geocode);
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid);
        }

        String page = base.requestLogged("http://www.geocaching.com/seek/cache_details.aspx", params, false, false, false);

        if (StringUtils.isEmpty(page)) {
            if (app.isThere(geocode, guid, true, false)) {
                if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                    Log.i(Settings.tag, "Loading old cache from cache.");

                    geocode = app.getGeocode(guid);
                }

                final List<cgCache> cacheList = new ArrayList<cgCache>();
                cacheList.add(app.getCacheByGeocode(geocode));
                search.addGeocode(geocode);
                search.error = null;

                app.addSearch(search, cacheList, false, reason);

                cacheList.clear();

                return search.getCurrentId();
            }

            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No data from server");
            return null;
        }

        final cgCacheWrap caches = base.parseCache(page, reason);
        if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
            if (caches != null && StringUtils.isNotBlank(caches.error)) {
                search.error = caches.error;
            }
            if (caches != null && StringUtils.isNotBlank(caches.url)) {
                search.url = caches.url;
            }

            app.addSearch(search, null, true, reason);

            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No cache parsed");
            return null;
        }

        if (app == null) {
            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No application found");
            return null;
        }

        List<cgCache> cacheList = cgBase.processSearchResults(search, caches, false, false, null);
        app.addSearch(search, cacheList, true, reason);

        return search.getCurrentId();
    }
}
