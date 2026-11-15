package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import static cgeo.geocaching.sorting.VisitedCache.JAN_2024;
import static cgeo.geocaching.sorting.VisitedCache.MAR_2024;
import static cgeo.geocaching.sorting.VisitedCache.NOV_2023;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class VisitComparatorTest {

    private final VisitComparator comp = VisitComparator.singleton;

    @Test
    public void testSortIgnoresOfflineLog() {
        final Geocache visitedOld = new VisitedCache("GC1", NOV_2023, MAR_2024);
        final Geocache visitedNew = new VisitedCache("GC2", JAN_2024, 0);
        final Geocache offlineLogOnly = new VisitedCache("GC3", 0, MAR_2024);

        final List<Geocache> caches = new ArrayList<>(Arrays.asList(offlineLogOnly, visitedOld, visitedNew));
        comp.sort(caches);
        assertThat(caches).containsExactly(visitedNew, visitedOld, offlineLogOnly);
    }

    @Test
    public void testSortableSectionIgnoresOfflineLog() {
        assertThat(comp.getSortableSection(new VisitedCache("GC1", NOV_2023, MAR_2024))).isEqualTo("2023-11");
        assertThat(comp.getSortableSection(new VisitedCache("GC1", 0, MAR_2024))).isEmpty();
    }
}
