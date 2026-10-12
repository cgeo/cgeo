package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.al.ALConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.storage.DataStore;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GCWebAPITest {

    private static final GCConnector GC_CONNECTOR = GCConnector.getInstance();

    @Test
    public void nullResultPageStopsOnlyGcPagination() {
        final SearchResult page = GCWebAPI.searchCaches(GC_CONNECTOR, new StubSearch(null, 1000, 50, 200));

        assertEmptyPage(page);

        final SearchResult accumulated = new SearchResult(new HashSet<>(Arrays.asList("GCSEARCHPAGE1", "GCSEARCHPAGE2")));
        accumulated.setLeftToFetch(GC_CONNECTOR, 50);
        accumulated.setLeftToFetch(ALConnector.getInstance(), 7);
        accumulated.addSearchResult(page);

        final int gcLeftToFetch = accumulated.getFromContext(GC_CONNECTOR,
                bundle -> bundle.getInt(SearchResult.CON_LEFT_TO_FETCH, -1));
        final int alLeftToFetch = accumulated.getFromContext(ALConnector.getInstance(),
                bundle -> bundle.getInt(SearchResult.CON_LEFT_TO_FETCH, -1));
        assertThat(gcLeftToFetch).isZero();
        assertThat(alLeftToFetch).isEqualTo(7);
        assertThat(accumulated.getTotalCount()).isEqualTo(accumulated.getCount() + 7);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // assertions are in assertEmptyPage
    public void emptyResultPageStopsPagination() {
        final SearchResult page = GCWebAPI.searchCaches(GC_CONNECTOR,
                new StubSearch(Collections.emptyList(), 1000, 50, 200));

        assertEmptyPage(page);
    }

    @Test
    public void nonEmptyFullPageRemainsPartial() {
        final String geocode = "GCSEARCHPAGE";
        final GCWebAPI.MapSearchResult cacheResult = new GCWebAPI.MapSearchResult();
        cacheResult.code = geocode;
        cacheResult.name = "Search page cache";
        cacheResult.postedCoordinates = new GCWebAPI.PostedCoordinates(50, 8);

        try {
            final SearchResult page = GCWebAPI.searchCaches(GC_CONNECTOR,
                    new StubSearch(Collections.singletonList(cacheResult), 3, 1, 0));

            assertThat(page.getError()).isEqualTo(StatusCode.NO_ERROR);
            assertThat(page.getGeocodes()).containsExactly(geocode);
            assertThat(page.getPartialConnectors()).containsExactly(GC_CONNECTOR.getName());
            assertThat(page.getTotalCount()).isEqualTo(3);
        } finally {
            DataStore.removeCache(geocode, EnumSet.of(LoadFlags.RemoveFlag.CACHE));
        }
    }

    private static void assertEmptyPage(final SearchResult page) {
        assertThat(page.getError()).isEqualTo(StatusCode.NO_ERROR);
        assertThat(page.getCount()).isZero();
        assertThat(page.getPartialConnectors()).doesNotContain(GC_CONNECTOR.getName());
        final int leftToFetch = page.getFromContext(GC_CONNECTOR,
                bundle -> bundle.getInt(SearchResult.CON_LEFT_TO_FETCH, -1));
        assertThat(leftToFetch).isZero();
        assertThat(page.getTotalCount()).isZero();
    }

    private static class StubSearch extends GCWebAPI.WebApiSearch {

        private final GCWebAPI.MapSearchResultSet resultSet = new GCWebAPI.MapSearchResultSet();

        StubSearch(final List<GCWebAPI.MapSearchResult> results, final int total, final int take, final int skip) {
            resultSet.results = results;
            resultSet.total = total;
            setPage(take, skip);
        }

        @Override
        GCWebAPI.MapSearchResultSet execute() {
            return resultSet;
        }
    }
}
