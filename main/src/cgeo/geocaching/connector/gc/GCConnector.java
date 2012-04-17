package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.network.Login;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
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
        if (geocode == null) {
            return false;
        }
        return GCConstants.PATTERN_GC_CODE.matcher(geocode).matches();
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
    public SearchResult searchByGeocode(final String geocode, final String guid, final cgeoapplication app, final CancellableHandler handler) {

        if (app == null) {
            Log.e("cgeoBase.searchByGeocode: No application found");
            return null;
        }

        final Parameters params = new Parameters("decrypt", "y");
        if (StringUtils.isNotBlank(geocode)) {
            params.put("wp", geocode);
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid);
        }
        params.put("log", "y");
        params.put("numlogs", String.valueOf(GCConstants.NUMBER_OF_LOGS));

        cgBase.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final String page = Login.getRequestLogged("http://www.geocaching.com/seek/cache_details.aspx", params);

        if (StringUtils.isEmpty(page)) {
            final SearchResult search = new SearchResult();
            if (app.isThere(geocode, guid, true, false)) {
                if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                    Log.i("Loading old cache from cache.");

                    search.addGeocode(app.getGeocode(guid));
                } else {
                    search.addGeocode(geocode);
                }
                search.setError(StatusCode.NO_ERROR);
                return search;
            }

            Log.e("cgeoBase.searchByGeocode: No data from server");
            search.setError(StatusCode.COMMUNICATION_ERROR);
            return search;
        }

        final SearchResult searchResult = cgBase.parseCache(page, handler);

        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.e("cgeoBase.searchByGeocode: No cache parsed");
            return searchResult;
        }

        final SearchResult search = searchResult.filterSearchResults(false, false, Settings.getCacheType());

        return search;
    }

    @Override
    public SearchResult searchByGeocodes(Set<String> geocodes) {
        return GCBase.searchByGeocodes(geocodes);
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String[] tokens) {
        return GCBase.searchByViewport(viewport, tokens);
    }

    @Override
    public boolean isZippedGPXFile(final String fileName) {
        return gpxZipFilePattern.matcher(fileName).matches();
    }

    @Override
    public boolean isReliableLatLon(boolean cacheHasReliableLatLon) {
        return cacheHasReliableLatLon;
    }

    @Override
    public String[] getTokens() {
        return GCBase.getTokens();
    }
}
