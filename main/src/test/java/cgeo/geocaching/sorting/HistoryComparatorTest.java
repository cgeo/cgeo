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

public class HistoryComparatorTest {

    private final HistoryComparator comp = HistoryComparator.singleton;

    @Test
    public void testHistoryDateVisitedOnly() {
        assertThat(new VisitedCache("GC1", NOV_2023, 0).getHistoryDate()).isEqualTo(NOV_2023);
    }

    @Test
    public void testHistoryDateOfflineLogOnly() {
        assertThat(new VisitedCache("GC1", 0, JAN_2024).getHistoryDate()).isEqualTo(JAN_2024);
    }

    @Test
    public void testHistoryDateOfflineLogNewer() {
        assertThat(new VisitedCache("GC1", NOV_2023, JAN_2024).getHistoryDate()).isEqualTo(JAN_2024);
    }

    @Test
    public void testHistoryDateOfflineLogOlder() {
        assertThat(new VisitedCache("GC1", JAN_2024, NOV_2023).getHistoryDate()).isEqualTo(JAN_2024);
    }

    @Test
    public void testHistoryDateNone() {
        assertThat(new VisitedCache("GC1", 0, 0).getHistoryDate()).isZero();
    }

    @Test
    public void testSortLatestFirst() {
        final Geocache visitedOnly = new VisitedCache("GC1", NOV_2023, 0);
        final Geocache offlineLogOnly = new VisitedCache("GC2", 0, JAN_2024);
        final Geocache offlineLogNewer = new VisitedCache("GC3", NOV_2023, MAR_2024);
        final Geocache none = new VisitedCache("GC4", 0, 0);

        final List<Geocache> caches = new ArrayList<>(Arrays.asList(none, visitedOnly, offlineLogOnly, offlineLogNewer));
        comp.sort(caches);
        assertThat(caches).containsExactly(offlineLogNewer, offlineLogOnly, visitedOnly, none);
    }

    @Test
    public void testSortableSection() {
        assertThat(comp.getSortableSection(new VisitedCache("GC1", NOV_2023, 0))).isEqualTo("2023-11");
        assertThat(comp.getSortableSection(new VisitedCache("GC1", 0, JAN_2024))).isEqualTo("2024-01");
        assertThat(comp.getSortableSection(new VisitedCache("GC1", NOV_2023, MAR_2024))).isEqualTo("2024-03");
        assertThat(comp.getSortableSection(new VisitedCache("GC1", 0, 0))).isEmpty();
    }
}
