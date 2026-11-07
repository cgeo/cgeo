package cgeo.geocaching.sorting;

import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * needs to be an instrumented test, as CacheListType requires the application
 */
public class GeocacheSortTest {

    private static final long NOV_2023 = 1_700_049_600_000L; // 2023-11-15
    private static final long JAN_2024 = 1_705_320_000_000L; // 2024-01-15

    /**
     * cache with given visited date and optional offline log, which does not need database access
     */
    private static class VisitedCache extends Geocache {

        @Nullable
        private final OfflineLogEntry offlineLog;

        VisitedCache(final String geocode, final long visitedDate, final long offlineLogDate) {
            setGeocode(geocode);
            setVisitedDate(visitedDate);
            setHasLogOffline(offlineLogDate > 0);
            offlineLog = offlineLogDate > 0 ? new OfflineLogEntry.Builder().setDate(offlineLogDate).build() : null;
        }

        @Override
        @Nullable
        public OfflineLogEntry getOfflineLog() {
            return offlineLog;
        }
    }

    private final Geocache visited = new VisitedCache("GC1", NOV_2023, 0);
    private final Geocache offlineLogOnly = new VisitedCache("GC2", 0, JAN_2024);

    private List<Geocache> sort(final CacheListType listType, final GeocacheSort.SortType type, final boolean inverse) {
        final GeocacheSort sort = new GeocacheSort();
        sort.setListType(listType);
        sort.setType(type, inverse);
        final List<Geocache> caches = new ArrayList<>(Arrays.asList(visited, offlineLogOnly));
        sort.getComparator().sort(caches);
        return caches;
    }

    @Test
    public void testLoggedDateInHistoryConsidersOfflineLog() {
        assertThat(sort(CacheListType.HISTORY, GeocacheSort.SortType.LOGGED_DATE, false)).containsExactly(offlineLogOnly, visited);
        assertThat(sort(CacheListType.HISTORY, GeocacheSort.SortType.LOGGED_DATE, true)).containsExactly(visited, offlineLogOnly);
    }

    @Test
    public void testAutoInHistoryConsidersOfflineLog() {
        assertThat(sort(CacheListType.HISTORY, GeocacheSort.SortType.AUTO, false)).containsExactly(offlineLogOnly, visited);
    }

    @Test
    public void testLoggedDateInOtherListIgnoresOfflineLog() {
        assertThat(sort(CacheListType.OFFLINE, GeocacheSort.SortType.LOGGED_DATE, false)).containsExactly(visited, offlineLogOnly);
        assertThat(sort(CacheListType.OFFLINE, GeocacheSort.SortType.LOGGED_DATE, true)).containsExactly(offlineLogOnly, visited);
    }
}
