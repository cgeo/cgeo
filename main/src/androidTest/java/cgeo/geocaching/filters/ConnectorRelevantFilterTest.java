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
import cgeo.geocaching.filters.core.OfflineLogGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sorting.GeocacheSort;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Instrumented API test for the combination of OriginGeocacheFilter and OwnerGeocacheFilter.
 * Tests OR(AND(Origin=GC, Owner="eddiemuc"), AND(Origin=OC, Owner="lineflyer")) against the
 * real geocaching.com and opencaching.de APIs.
 * <p>
 * Requires configured accounts for geocaching.com and opencaching.de on the emulator/device.
 */
public class ConnectorRelevantFilterTest {
    private static final GCConnector GC = GCConnector.getInstance();
    private static final OCDEConnector OC = new OCDEConnector();
    private static final String GC_TEST_OWNER = "eddiemuc";
    private static final String OC_TEST_OWNER = "lineflyer";

    @Test
    public void searchGCOrOCFilter() {
        // OR(AND(Origin=GC, Owner=GC_TEST_OWNER), AND(Origin=OC, Owner=OC_TEST_OWNER))
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, createGCOrOCFilter());

        // Sent to GC API: only GC branch is relevant → Owner=GC_TEST_OWNER
        final SearchResult gcSearch = GC.searchByFilter(filter, new GeocacheSort());
        assertThat(gcSearch).isNotNull();
        assertThat(gcSearch.getCount()).isNotEqualTo(0);
        assertOwner(gcSearch, GC_TEST_OWNER);

        // Sent to OC API: only OC branch is relevant → Owner=OC_TEST_OWNER
        final SearchResult ocSearch = OC.searchByFilter(filter, new GeocacheSort());
        assertThat(ocSearch).isNotNull();
        assertThat(ocSearch.getCount()).isNotEqualTo(0);
        assertOwner(ocSearch, OC_TEST_OWNER);
    }

    @Test
    public void searchIgnoreOCFilter() {
        // AND(NOT(Origin=OC), Owner=OC_TEST_OWNER))
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, createIgnoreOCFilter());

        // Sent to GC API: only GC branch is relevant → Owner=OC_TEST_OWNER
        final SearchResult gcSearch = GC.searchByFilter(filter, new GeocacheSort());
        assertThat(gcSearch).isNotNull();
        assertThat(gcSearch.getCount()).isNotEqualTo(0);
        assertOwner(gcSearch, OC_TEST_OWNER);

        // Sent to OC API: OC branch is not relevant → empty SearchResult
        final SearchResult ocSearch = OC.searchByFilter(filter, new GeocacheSort());
        assertThat(ocSearch).isNotNull();
        assertThat(ocSearch.getGeocodes()).isEmpty();
    }

    private static void assertOwner(final SearchResult search, final String expectedOwner) {
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

    @Test
    public void searchGCWithOfflineOnlyStatusInAndChain() {
        // AND(Origin=GC, Owner=GC_TEST_OWNER, Status(hasOfflineFoundLog=true))
        // The offline-only status sub-criterion must not prevent the GC API call from succeeding.
        // GC API uses only the Owner filter (STATUS with hasOfflineFoundLog is not in GC capabilities).
        final IGeocacheFilter filter = AndGeocacheFilter.create(
                createOriginFilter(GC),
                createOwnerFilter(GC_TEST_OWNER),
                createStatusWithOfflineFoundLog());
        final GeocacheFilter geocacheFilter = GeocacheFilter.create(null, false, false, filter);

        final SearchResult gcSearch = GC.searchByFilter(geocacheFilter, new GeocacheSort());
        assertThat(gcSearch).isNotNull();
        assertThat(gcSearch.getCount()).isNotEqualTo(0);
        assertOwner(gcSearch, GC_TEST_OWNER);
    }

    @Test
    public void searchGCOrOCFilterWithOfflineOnlyBranch() {
        // OR(AND(Origin=GC, Owner=GC_TEST_OWNER), AND(Origin=OC, OfflineLog="draft"))
        // GC: OC branch (with offline-only OfflineLog filter) is removed → only Owner=GC_TEST_OWNER remains
        // OC: GC branch removed; OfflineLog filter is not in OC capabilities → OC returns result without error
        final IGeocacheFilter filter = OrGeocacheFilter.create(
                createOwnerBranch(GC, GC_TEST_OWNER),
                createOfflineLogBranch(OC, "draft"));
        final GeocacheFilter geocacheFilter = GeocacheFilter.create(null, false, false, filter);

        // GC: offline-only OC branch must not interfere → normal owner result
        final SearchResult gcSearch = GC.searchByFilter(geocacheFilter, new GeocacheSort());
        assertThat(gcSearch).isNotNull();
        assertThat(gcSearch.getCount()).isNotEqualTo(0);
        assertOwner(gcSearch, GC_TEST_OWNER);

        // OC: must not throw; offline-only filter not pushed to API
        final SearchResult ocSearch = OC.searchByFilter(geocacheFilter, new GeocacheSort());
        assertThat(ocSearch).isNotNull();
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

    private static IGeocacheFilter createStatusWithOfflineFoundLog() {
        final StatusGeocacheFilter statusFilter = GeocacheFilterType.STATUS.create();
        statusFilter.setStatusHasOfflineFoundLog(true);
        return statusFilter;
    }

    private static IGeocacheFilter createOfflineLogBranch(final IConnector connector, final String logText) {
        final OfflineLogGeocacheFilter offlineLogFilter = GeocacheFilterType.OFFLINE_LOG.create();
        offlineLogFilter.getStringFilter().setTextValue(logText);
        return AndGeocacheFilter.create(createOriginFilter(connector), offlineLogFilter);
    }
}
