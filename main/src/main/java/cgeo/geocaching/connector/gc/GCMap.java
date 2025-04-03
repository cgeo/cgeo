package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.filters.core.AttributesGeocacheFilter;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyAndTerrainGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyTerrainMatrixGeocacheFilter;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.FavoritesGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.HiddenGeocacheFilter;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.filters.core.SizeGeocacheFilter;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.filters.core.StringFilter;
import cgeo.geocaching.filters.core.TerrainGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter.Format;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.GeocacheSort;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.Log;

import android.os.Bundle;

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
    public static SearchResult searchByViewport(final IConnector con, @NonNull final Viewport viewport, @Nullable final GeocacheFilter filter) {
        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "GCMap.searchByViewport")) {
            cLog.add("vp:" + viewport);

            final GCWebAPI.WebApiSearch search = createSearchForFilter(con, filter);
            if (search == null) {
                return new SearchResult();
            }

            search.setBox(viewport);
            search.setPage(500, 0);

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
    public static SearchResult searchByFilter(@NonNull final IConnector con, @NonNull final GeocacheFilter filter, @NonNull final GeocacheSort sort) {
        return searchByFilter(con, filter, sort, SEARCH_LOAD_INITIAL, 0);
    }

    @Nullable
    @WorkerThread
    public static SearchResult searchByNextPage(final IConnector con, final Bundle context, final GeocacheFilter filter, final GeocacheSort sort) {
        final int alreadyTook = context.getInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, 0);
        return searchByFilter(con, filter, sort, SEARCH_LOAD_NEXTPAGE, alreadyTook);
    }

    @WorkerThread
    private static SearchResult searchByFilter(final IConnector con, @NonNull final GeocacheFilter filter, @NonNull final GeocacheSort sort, final int take, final int alreadyTook) {

        final GCWebAPI.WebApiSearch search = createSearchForFilter(con, filter, sort, take, alreadyTook);
        if (search == null) {
            return new SearchResult();
        }

        final SearchResult sr = GCWebAPI.searchCaches(con, search, true);
        search.fillSearchCacheData(sr.getOrCreateCacheData());
        sr.setToContext(con, b -> b.putString(GCConnector.SEARCH_CONTEXT_FILTER, filter.toConfig()));
        sr.setToContext(con, b -> b.putParcelable(GCConnector.SEARCH_CONTEXT_SORT, sort));
        sr.setToContext(con, b -> b.putInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, take + alreadyTook));
        return sr;
    }


    private static GCWebAPI.WebApiSearch createSearchForFilter(final IConnector connector, @Nullable final GeocacheFilter filter) {
        return createSearchForFilter(connector, filter, new GeocacheSort(), 200, 0);
    }

    private static GCWebAPI.WebApiSearch createSearchForFilter(final IConnector connector, @Nullable final GeocacheFilter pFilter, @NonNull final GeocacheSort sort, final int take, final int skip) {

        final GCWebAPI.WebApiSearch search = new GCWebAPI.WebApiSearch();
        search.setPage(take, skip);

        final GeocacheFilter filter = pFilter != null ? pFilter : GeocacheFilter.createEmpty();

        //special case: if origin filter is present and excludes GCConnector, then skip search
        final OriginGeocacheFilter origin = GeocacheFilter.findInChain(filter.getAndChainIfPossible(), OriginGeocacheFilter.class);
        if (origin != null && !origin.allowsCachesOf(connector)) {
            return null;
        }

        final List<BaseGeocacheFilter> filterAndChain = filter.getAndChainIfPossible();
        for (BaseGeocacheFilter baseFilter : filterAndChain) {
            fillForBasicFilter(baseFilter, search);
        }

        search.setSort(GCWebAPI.WebApiSearch.SortType.getByCGeoSortType(sort.getEffectiveType()), sort.isEffectiveAscending());

        return search;

    }

    private static void fillForBasicFilter(@NonNull final BaseGeocacheFilter basicFilter, final GCWebAPI.WebApiSearch search) {
        switch (basicFilter.getType()) {
            case TYPE:
                search.addCacheTypes(((TypeGeocacheFilter) basicFilter).getRawValues());
                break;
            case NAME:
                if (((NameGeocacheFilter) basicFilter).getStringFilter().getFilterType() != StringFilter.StringFilterType.DOES_NOT_CONTAIN) {
                    search.setKeywords(((NameGeocacheFilter) basicFilter).getStringFilter().getTextValue());
                }
                break;
            case ATTRIBUTES:
                final AttributesGeocacheFilter attFilter = (AttributesGeocacheFilter) basicFilter;
                if (!attFilter.isInverse()) {
                    search.addCacheAttributes(
                        CollectionStream.of(attFilter.getAttributes().entrySet())
                        .filter(e -> Boolean.TRUE.equals(e.getValue()))
                        .filter(e -> e.getKey().gcid >= 0 && e.getKey().gcid < 100)
                        .map(Map.Entry::getKey)
                        .toArray(CacheAttribute.class));
                }
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
            case DIFFICULTY_TERRAIN_MATRIX:
                final DifficultyTerrainMatrixGeocacheFilter matrixFilter = (DifficultyTerrainMatrixGeocacheFilter) basicFilter;
                if (matrixFilter.isFilteringMatrix()) {
                    search.setDifficultyTerrainCombis(((DifficultyTerrainMatrixGeocacheFilter) basicFilter).getDtCombis());
                }
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
                search.setStatusEnabled(
                        statusFilter.isExcludeDisabled() && statusFilter.isExcludeArchived() ? Boolean.TRUE :
                                (statusFilter.isExcludeActive() && statusFilter.isExcludeArchived() ? FALSE : null));
                search.setShowArchived(!statusFilter.isExcludeArchived());
                break;
            case HIDDEN:
            case EVENT_DATE:
                final HiddenGeocacheFilter hiddenFilter = (HiddenGeocacheFilter) basicFilter;
                search.setPlacementDate(hiddenFilter.getMinDate(), hiddenFilter.getMaxDate());
                break;
            case LOG_ENTRY:
                final LogEntryGeocacheFilter foundByFilter = (LogEntryGeocacheFilter) basicFilter;
                if (foundByFilter.isInverse()) {
                    search.addNotFoundBy(foundByFilter.getFoundByUser());
                } else {
                    search.addFoundBy(foundByFilter.getFoundByUser());
                }
                break;
            default:
                break;
        }
    }
}
