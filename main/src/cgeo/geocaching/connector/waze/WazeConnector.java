package cgeo.geocaching.connector.waze;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.capability.IgnoreCapability;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;


public class WazeConnector extends AbstractConnector implements ISearchByCenter, ISearchByViewPort, IgnoreCapability {

    @Override
    public boolean canIgnoreCache(@NonNull Geocache cache) {
        return false;
    }

    @Override
    public void ignoreCache(@NonNull Geocache cache) {

    }

    private static class Holder {
        @NonNull
        private static final WazeConnector INSTANCE = new WazeConnector();
    }

    public static WazeConnector getInstance() {
        return WazeConnector.Holder.INSTANCE;
    }

    private WazeConnector()
    {}

    @Override
    public boolean isActive() {
        return Settings.isWazeConnectorActive();
    }

    @Override
    public boolean canHandle(@NonNull String geocode) {
        return StringUtils.startsWithAny(StringUtils.upperCase(geocode), "WAZ");
    }

    @NonNull
    @Override
    protected String getCacheUrlPrefix() {
        return null;
    }

    @Override
    public SearchResult searchByCenter(@NonNull Geopoint center) {
        final Collection<Geocache> caches = WazeAPI.searchByCenter( center);
        //final SearchResult searchResult = new SearchResult(caches);
        final SearchResult searchResult = new SearchResult();
        searchResult.addAndPutInCache(caches);
        return searchResult;
    }

    @NonNull
    @Override
    public SearchResult searchByViewport(@NonNull Viewport viewport) {
        final Collection<Geocache> caches = WazeAPI.searchByBBox(viewport);

        final SearchResult searchResult = new SearchResult();
        searchResult.addAndPutInCache(caches);
        return searchResult;
    }

    @NonNull
    @Override
    public String getName() {
        return "Waze";
    }

    @Nullable
    @Override
    public String getCacheUrl(@NonNull Geocache cache) {
        return null;
    }

    @NonNull
    @Override
    public String getHost() {
        return "www.waze.com";
    }

    @Override
    public boolean isOwner(@NonNull Geocache cache) {
        return false;
    }

    @NonNull
    @Override
    public String getNameAbbreviated() {
        return "WAZ";
    }
}
