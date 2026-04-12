package cgeo.geocaching.filters;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.oc.OCDEConnector;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.NotGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sorting.GeocacheSort;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Instrumented API test for the combination of OriginGeocacheFilter and OwnerGeocacheFilter.
 * Tests OR(AND(Origin=GC, Owner="lineflyer"), AND(Origin=OC, Owner="mic@")) against the
 * real geocaching.com and opencaching.de APIs.
 * <p>
 * Requires configured accounts for geocaching.com and opencaching.de on the emulator/device.
 */
public class ConnectorRelevantFilterTest {
    private static final GCConnector GC = GCConnector.getInstance();
    private static final OCDEConnector OC = new OCDEConnector();
    private static final String GC_TEST_OWNER = "Grokky Grokson";
    private static final String OC_TEST_OWNER = "lineflyer";

    @Test
    public void searchGCOrOCFilter() {
        // OR(AND(Origin=GC, Owner=GC_TEST_OWNER), AND(Origin=OC, Owner=OC_TEST_OWNER))
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, createGCOrOCFilter());

        // Sent to GC API: only GC branch is relevant → Owner=GC_TEST_OWNER
        final SearchResult gcSearch = GC.searchByFilter(filter, new GeocacheSort());
        assertThat(gcSearch).isNotNull();
        assertThat(gcSearch.getGeocodes()).isNotEmpty();
        assertOwner(gcSearch, GC_TEST_OWNER);

        // Sent to OC API: only OC branch is relevant → Owner=OC_TEST_OWNER
        final SearchResult ocSearch = OC.searchByFilter(filter, new GeocacheSort());
        assertThat(ocSearch).isNotNull();
        assertThat(ocSearch.getGeocodes()).isNotEmpty();
        assertOwner(ocSearch, OC_TEST_OWNER);
    }

    @Test
    public void searchIgnoreOCFilter() {
        // AND(NOT(Origin=OC), Owner=OC_TEST_OWNER))
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, createIgnoreOCFilter());

        // Sent to GC API: only GC branch is relevant → Owner=OC_TEST_OWNER
        final SearchResult gcSearch = GC.searchByFilter(filter, new GeocacheSort());
        assertThat(gcSearch).isNotNull();
        assertThat(gcSearch.getGeocodes()).isNotEmpty();
        assertOwner(gcSearch, OC_TEST_OWNER);

        // Sent to OC API: OC branch is not relevant → empty SearchResult
        final SearchResult ocSearch = OC.searchByFilter(filter, new GeocacheSort());
        assertThat(ocSearch).isNotNull();
        assertThat(ocSearch.getGeocodes()).isEmpty();
    }

    private static void assertOwner(SearchResult search, final String expectedOwner) {
        // Verify all caches belong to the expected owner
        final Set<Geocache> caches = search.getCachesFromSearchResult();
        assertThat(caches).isNotEmpty();
        for (final Geocache cache : caches) {
            assertThat(cache.getOwnerDisplayName())
                    .as("Owner of cache " + cache.getGeocode())
                    .isEqualToIgnoringCase(expectedOwner);
        }
    }

    private static IGeocacheFilter createGCOrOCFilter() {
        // OR(AND(Origin=GC, Owner=GC_TEST_OWNER), AND(Origin=OC, Owner=OC_TEST_OWNER))
        return OrGeocacheFilter.create(createOwnerBranch(GC, GC_TEST_OWNER), createOwnerBranch(OC, OC_TEST_OWNER));
    }

    private static IGeocacheFilter createIgnoreOCFilter() {
        // AND(NOT(Origin=OC), Owner=OC_TEST_OWNER))
        return AndGeocacheFilter.create(NotGeocacheFilter.create(createOriginFilter(OC)), createOwnerFilter(OC_TEST_OWNER));
    }

    private static IGeocacheFilter createOwnerBranch(final IConnector connector, final String owner) {
        return AndGeocacheFilter.create(createOriginFilter(connector), createOwnerFilter(owner));
    }

    private static IGeocacheFilter createOriginFilter(final IConnector connector) {
        final OriginGeocacheFilter originFilter = GeocacheFilterType.ORIGIN.create();
        originFilter.setValues(Collections.singletonList(connector));
        return originFilter;
    }

    private static IGeocacheFilter createOwnerFilter(final String owner) {
        final OwnerGeocacheFilter ownerFilter = GeocacheFilterType.OWNER.create();
        ownerFilter.getStringFilter().setTextValue(owner);
        return ownerFilter;
    }

}