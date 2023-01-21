package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.AttributesGeocacheFilter;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyAndTerrainGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyGeocacheFilter;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.FavoritesGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.HiddenGeocacheFilter;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.filters.core.SizeGeocacheFilter;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.filters.core.TerrainGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter.Format;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.filters.core.GeocacheFilterType.LOG_ENTRY;

import android.os.Bundle;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.List;
import java.util.Map;
import static java.lang.Boolean.FALSE;

public class GCMap {

    private static final int SEARCH_LOAD_INITIAL = 200;
    private static final int SEARCH_LOAD_NEXTPAGE = 50;

    private GCMap() {
        // utility class
    }

    /**
     * Searches the view port on the live map with Strategy.AUTO
     *
     * @param viewport Area to search
     */
    @NonNull
    @WorkerThread
    public static SearchResult searchByViewport(final IConnector con, @NonNull final Viewport viewport) {
        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "GCMap.searchByViewport")) {
            cLog.add("vp:" + viewport);

            final Pair<GCWebAPI.WebApiSearch, SearchResult> searchPair = createSearchForFilter(con, GeocacheFilterContext.getForType(GeocacheFilterContext.FilterType.LIVE));
            if (searchPair == null || searchPair.first == null) {
                return new SearchResult();
            }
            final GCWebAPI.WebApiSearch search = searchPair.first;
            search.setBox(viewport);

            final SearchResult searchResult = GCWebAPI.searchCaches(con, search, false);

            if (Settings.isDebug()) {
                searchResult.setUrl(con, viewport.getCenter().format(Format.LAT_LON_DECMINUTE));
            }
            cLog.add("returning " + searchResult.getCount() + " caches");
            return searchResult;
        }
    }

    @NonNull
    @WorkerThread
    public static SearchResult searchByFilter(@NonNull final IConnector con, @NonNull final GeocacheFilter filter) {
        return searchByFilter(con, filter, SEARCH_LOAD_INITIAL, 0);
    }

    @Nullable
    @WorkerThread
    public static SearchResult searchByNextPage(final IConnector con, final Bundle context, final GeocacheFilter filter) {
        final int alreadyTook = context.getInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, 0);
        return searchByFilter(con, filter, SEARCH_LOAD_NEXTPAGE, alreadyTook);
    }

    @WorkerThread
    private static SearchResult searchByFilter(final IConnector con, @NonNull final GeocacheFilter filter, final int take, final int alreadyTook) {

        final Pair<GCWebAPI.WebApiSearch, SearchResult> search = createSearchForFilter(con, filter, take, alreadyTook);
        if (search == null) {
            return new SearchResult();
        }
        if (search.first == null) {
            //this happens for legacy searches (e.g. by finder). Make sure filter is set there too for origin filtering
            search.second.setToContext(con, b -> b.putString(GCConnector.SEARCH_CONTEXT_FILTER, filter.toConfig()));
            return search.second;
        }

        final SearchResult sr = GCWebAPI.searchCaches(con, search.first, true);
        sr.setToContext(con, b -> b.putString(GCConnector.SEARCH_CONTEXT_FILTER, filter.toConfig()));
        sr.setToContext(con, b -> b.putInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, take + alreadyTook));
        return sr;
    }


    private static Pair<GCWebAPI.WebApiSearch, SearchResult> createSearchForFilter(final IConnector connector, @NonNull final GeocacheFilter filter) {
        return createSearchForFilter(connector, filter, 200, 0);
    }

    private static Pair<GCWebAPI.WebApiSearch, SearchResult> createSearchForFilter(final IConnector connector, @NonNull final GeocacheFilter filter, final int take, final int skip) {

        final GCWebAPI.WebApiSearch search = new GCWebAPI.WebApiSearch();
        search.setOrigin(LocationDataProvider.getInstance().currentGeo().getCoords());
        search.setPage(take, skip);

        //special case: if origin filter is present and excludes GCConnector, then skip search
        final OriginGeocacheFilter origin = GeocacheFilter.findInChain(filter.getAndChainIfPossible(), OriginGeocacheFilter.class);
        if (origin != null && !origin.allowsCachesOf(connector)) {
            return null;
        }

        for (BaseGeocacheFilter baseFilter : filter.getAndChainIfPossible()) {
            //special case: search by finder (->not supported by WebAPISearch, fall back to Website parsing search)
            if (LOG_ENTRY.equals(baseFilter.getType()) && (baseFilter instanceof LogEntryGeocacheFilter) && (!((LogEntryGeocacheFilter) baseFilter).isInverse())) {
                return new Pair<>(null, searchByFinder(connector, ((LogEntryGeocacheFilter) baseFilter).getFoundByUser(), filter));
            }
            fillForBasicFilter(baseFilter, search);
        }
        return new Pair<>(search, null);

    }

    private static SearchResult searchByFinder(final IConnector con, final String userName, final GeocacheFilter filter) {
        final List<BaseGeocacheFilter> filters = filter.getAndChainIfPossible();
        final StatusGeocacheFilter statusFilter = GeocacheFilter.findInChain(filters, StatusGeocacheFilter.class);
        final boolean noFoundOwn = statusFilter != null && FALSE.equals(statusFilter.getStatusFound()) && FALSE.equals(statusFilter.getStatusOwned());

        final TypeGeocacheFilter typeFilter = GeocacheFilter.findInChain(filters, TypeGeocacheFilter.class);
        final CacheType ct = typeFilter != null && typeFilter.getValues().size() == 1 ? typeFilter.getValues().iterator().next() : null;
        return GCParser.searchByUsername(con, userName, ct, noFoundOwn);
    }

    private static void fillForBasicFilter(@NonNull final BaseGeocacheFilter basicFilter, final GCWebAPI.WebApiSearch search) {
        switch (basicFilter.getType()) {
            case TYPE:
                search.addCacheTypes(((TypeGeocacheFilter) basicFilter).getRawValues());
                break;
            case NAME:
                search.setKeywords(((NameGeocacheFilter) basicFilter).getStringFilter().getTextValue());
                break;
            case ATTRIBUTES:
                search.addCacheAttributes(
                        CollectionStream.of(((AttributesGeocacheFilter) basicFilter).getAttributes().entrySet())
                                .filter(e -> Boolean.TRUE.equals(e.getValue())).map(Map.Entry::getKey).toArray(CacheAttribute.class));
                break;
            case SIZE:
                search.addCacheSizes(((SizeGeocacheFilter) basicFilter).getValues());
                break;
            case DISTANCE:
                final DistanceGeocacheFilter distanceFilter = (DistanceGeocacheFilter) basicFilter;
                final Geopoint coord = distanceFilter.getEffectiveCoordinate();
                if (distanceFilter.getMaxRangeValue() != null) {
                    search.setBox(new Viewport(coord, distanceFilter.getMaxRangeValue()));
                } else {
                    search.setOrigin(coord);
                }
                break;
            case DIFFICULTY:
                search.setDifficulty(((DifficultyGeocacheFilter) basicFilter).getMinRangeValue(), ((DifficultyGeocacheFilter) basicFilter).getMaxRangeValue());
                break;
            case TERRAIN:
                search.setTerrain(((TerrainGeocacheFilter) basicFilter).getMinRangeValue(), ((TerrainGeocacheFilter) basicFilter).getMaxRangeValue());
                break;
            case DIFFICULTY_TERRAIN:
                fillForBasicFilter(((DifficultyAndTerrainGeocacheFilter) basicFilter).difficultyGeocacheFilter, search);
                fillForBasicFilter(((DifficultyAndTerrainGeocacheFilter) basicFilter).terrainGeocacheFilter, search);
                break;
            case RATING:
                // not supported for online searches for this connector
                break;
            case OWNER:
                search.setHiddenBy(((OwnerGeocacheFilter) basicFilter).getStringFilter().getTextValue());
                break;
            case FAVORITES:
                final FavoritesGeocacheFilter favFilter = (FavoritesGeocacheFilter) basicFilter;
                if (!favFilter.isPercentage() && favFilter.getMinRangeValue() != null) {
                    search.setMinFavoritepoints(Math.round(favFilter.getMinRangeValue()));
                }
                break;
            case STATUS:
                final StatusGeocacheFilter statusFilter = (StatusGeocacheFilter) basicFilter;
                search.setStatusFound(statusFilter.getStatusFound());
                search.setStatusOwn(statusFilter.getStatusOwned());
                search.setStatusEnabled(statusFilter.isExcludeDisabled() ? Boolean.TRUE : (statusFilter.isExcludeActive() ? FALSE : null));
                break;
            case HIDDEN:
                final HiddenGeocacheFilter hiddenFilter = (HiddenGeocacheFilter) basicFilter;
                search.setPlacementDate(hiddenFilter.getMinDate(), hiddenFilter.getMaxDate());
                break;
            case LOG_ENTRY:
                final LogEntryGeocacheFilter foundByFilter = (LogEntryGeocacheFilter) basicFilter;
                if (foundByFilter.isInverse()) {
                    search.setNotFoundBy(foundByFilter.getFoundByUser());
                }
                break;
            default:
                break;
        }
    }
}
