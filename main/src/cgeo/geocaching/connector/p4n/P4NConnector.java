package cgeo.geocaching.connector.p4n;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.capability.IgnoreCapability;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.DisposableHandler;


public class P4NConnector extends AbstractConnector implements ISearchByCenter, ISearchByViewPort,ISearchByGeocode, IgnoreCapability
{

    public static P4NConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public SearchResult searchByCenter(@NonNull Geopoint center) {
        final Collection<Geocache> caches = P4NApi.searchByCenterLite( center,(float)3);
        //final SearchResult searchResult = new SearchResult(caches);
        final SearchResult searchResult = new SearchResult();
        searchResult.addAndPutInCache(caches);
        return searchResult;
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull
        private static final P4NConnector INSTANCE = new P4NConnector();
    }

    private P4NConnector()
    {}

    @NonNull
    @Override
    protected String getCacheUrlPrefix() {
        return null;
    }

    @NonNull
    @Override
    public SearchResult searchByViewport(@NonNull Viewport viewport) {
        final Collection<Geocache> caches = P4NApi.searchByBBox(viewport);
        //final SearchResult searchResult = new SearchResult(caches);
       final SearchResult searchResult = new SearchResult();
        searchResult.addAndPutInCache(caches);
       return searchResult;
    }

    @NonNull
    @Override
    public String getName() {
        return "Park4night";
    }

    @Nullable
    @Override
    public String getCacheUrl(@NonNull Geocache cache) {
        return "https://park4night.com/lieu/"+cache.getCacheId()+"/";
    }

    @NonNull
    @Override
    public String getHost() {
        return "www.park4night.com";
    }

    @Override
    public boolean isOwner(@NonNull Geocache cache) {
        return false;
    }

    @NonNull
    @Override
    public String getNameAbbreviated() {
        return "P4N";
    }

    @Override
    public boolean isActive() {
        return Settings.isP4NConnectorActive();
    }

    @Override
    public SearchResult searchByGeocode(@Nullable String geocode, @Nullable String guid, DisposableHandler handler) {
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);
        final Geocache cache = P4NApi.searchById(geocode.substring(3));
        //final SearchResult searchResult = new SearchResult(cache);
        final SearchResult searchResult = new SearchResult();
        searchResult.addAndPutInCache(Collections.singletonList(cache));

        List<LogEntry> logEntries = P4NApi.GetLogs(geocode.substring(3));

        DataStore.saveLogs(cache.getGeocode(), logEntries, true);

        return searchResult;
    }

    @Override
    public boolean canHandle(@NonNull String geocode) {
        return StringUtils.startsWithAny(StringUtils.upperCase(geocode), "P4N");
    }

    @Override
    public boolean canIgnoreCache(@NonNull Geocache cache) {
        return false;
    }

    @Override
    public void ignoreCache(@NonNull Geocache cache) {

    }
}
