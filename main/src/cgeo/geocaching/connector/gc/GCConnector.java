package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgData;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.enumerations.CacheRealm;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class GCConnector extends AbstractConnector implements ISearchByGeocode, ISearchByCenter, ISearchByViewPort {

    private static final String HTTP_COORD_INFO = "http://coord.info/";
    private static final Pattern gpxZipFilePattern = Pattern.compile("\\d{7,}(_.+)?\\.zip", Pattern.CASE_INSENSITIVE);

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
        return GCConstants.PATTERN_GC_CODE.matcher(geocode).matches() || GCConstants.PATTERN_TB_CODE.matcher(geocode).matches();
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        // it would also be possible to use "http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.getGeocode();
        return "http://www.geocaching.com//seek/cache_details.aspx?wp=" + cache.getGeocode();
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
        return gpxZipFilePattern.matcher(fileName).matches();
    }

    @Override
    public boolean isReliableLatLon(boolean cacheHasReliableLatLon) {
        return cacheHasReliableLatLon;
    }

    public static boolean addToWatchlist(cgCache cache) {
        final boolean added = GCParser.addToWatchlist(cache);
        if (added) {
            cgData.saveChangedCache(cache);
        }
        return added;
    }

    public static boolean removeFromWatchlist(cgCache cache) {
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

    public static boolean addToFavorites(cgCache cache) {
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

    public static boolean removeFromFavorites(cgCache cache) {
        final boolean removed = GCParser.removeFromFavorites(cache);
        if (removed) {
            cgData.saveChangedCache(cache);
        }
        return removed;
    }

    @Override
    public boolean uploadModifiedCoordinates(cgCache cache, Geopoint wpt) {
        final boolean uploaded = GCParser.uploadModifiedCoordinates(cache, wpt);
        if (uploaded) {
            cgData.saveChangedCache(cache);
        }
        return uploaded;
    }

    @Override
    public boolean deleteModifiedCoordinates(cgCache cache) {
        final boolean deleted = GCParser.deleteModifiedCoordinates(cache);
        if (deleted) {
            cgData.saveChangedCache(cache);
        }
        return deleted;
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
        return HTTP_COORD_INFO;
    }

    @Override
    public CacheRealm getCacheRealm() {
        return CacheRealm.GC;
    }

    @Override
    public boolean isActivated() {
        return true;
    }
}
