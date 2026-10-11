package cgeo.geocaching.filters;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocodeGeocacheFilter;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import java.util.Arrays;
import java.util.EnumSet;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LogEntryGeocacheFilterTest {

    private static final String GEOCODE = "GCF2LOGFILTER";
    private static final String AUTHOR = "Alice";

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // assertions are in assertStoredFilter
    public void locallyStoredLogsKeepLogicAndSqlFilteringInSync() {
        final Geocache cache = new Geocache();
        cache.setGeocode(GEOCODE);
        DataStore.removeCache(GEOCODE, EnumSet.of(LoadFlags.RemoveFlag.CACHE, LoadFlags.RemoveFlag.DB));

        try {
            DataStore.storeIntoDatabase(cache);
            DataStore.saveLogs(GEOCODE, Arrays.asList(
                    new LogEntry.Builder().setAuthor(AUTHOR).setLog("contains needle here").build(),
                    new LogEntry.Builder().setAuthor("Other").setLog("other text").build()), true);

            // SQL filtering intentionally evaluates only the log rows currently stored in the local database.
            assertStoredFilter(cache, false, null, true);
            assertStoredFilter(cache, false, "needle", true);
            assertStoredFilter(cache, false, "missing", false);
            assertStoredFilter(cache, true, "needle", false);
            assertStoredFilter(cache, true, "missing", true);
        } finally {
            DataStore.removeCache(GEOCODE, EnumSet.of(LoadFlags.RemoveFlag.CACHE, LoadFlags.RemoveFlag.DB));
        }
    }

    private static void assertStoredFilter(final Geocache cache, final boolean inverse, final String logText, final boolean expected) {
        final LogEntryGeocacheFilter logFilter = new LogEntryGeocacheFilter();
        logFilter.setFoundByUser(AUTHOR);
        logFilter.setLogText(logText);
        logFilter.setInverse(inverse);

        assertThat(logFilter.filter(cache)).as("in-memory filter").isEqualTo(expected);

        final GeocodeGeocacheFilter geocodeFilter = new GeocodeGeocacheFilter();
        geocodeFilter.getStringFilter().setTextValue(GEOCODE);
        final AndGeocacheFilter filterTree = new AndGeocacheFilter();
        filterTree.addChild(geocodeFilter);
        filterTree.addChild(logFilter);
        final SearchResult result = DataStore.getBatchOfStoredCaches(null, -1,
                GeocacheFilter.create(false, false, filterTree), null, false, 1);

        assertThat(result.getGeocodes().contains(GEOCODE)).as("database filter").isEqualTo(expected);
    }
}
