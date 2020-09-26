package cgeo.geocaching.filter;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.models.Geocache;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SizeFilterTest {

    private Geocache micro;
    private Geocache regular;
    private SizeFilter microFilter;

    @Before
    public void setUp() throws Exception {
        // cache initialization can only be done without errors after application setup
        micro = new Geocache();
        micro.setSize(CacheSize.MICRO);

        regular = new Geocache();
        regular.setSize(CacheSize.REGULAR);

        microFilter = new SizeFilter(CacheSize.MICRO);
    }

    @Test
    public void testAccepts() {
        assertThat(microFilter.accepts(micro)).isTrue();
        assertThat(microFilter.accepts(regular)).isFalse();
    }

    @Test
    public void testGetAllFilters() {
        final int expectedSizes = CacheSize.values().length - 1; // hide "UNKNOWN"
        assertThat(new SizeFilter.Factory().getFilters()).hasSize(expectedSizes);
    }

    @Test
    public void testFilter() {
        final ArrayList<Geocache> list = new ArrayList<>();
        list.add(regular);
        list.add(micro);
        assertThat(list).hasSize(2);

        microFilter.filter(list);
        assertThat(list).hasSize(1);
    }

    @Test(timeout = 5000) //filtering large lists in memory should take only MILLISECONDS. we set a very generous timeout of 5s here
    public void testFilteringPerformanceOnLargeLists() {
        //filter a very large list such that is becomes a very short list
        final List<Geocache> list = new ArrayList<>();
        for (int i = 0; i < 50000; i++) {
            final Geocache regularGc = new Geocache();
            regularGc.setGeocode("GCFAKE" + i);
            regularGc.setSize(CacheSize.REGULAR);
            list.add(regularGc);
            if (i % 1000 == 0) {
                final Geocache microGc = new Geocache();
                microGc.setGeocode("GCFAKE" + i);
                microGc.setSize(CacheSize.MICRO);
                list.add(micro);
            }
        }
        assertThat(list).hasSize(50000 + 50);

        microFilter.filter(list);
        assertThat(list).hasSize(50);
    }

}
