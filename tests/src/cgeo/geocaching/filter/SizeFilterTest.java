package cgeo.geocaching.filter;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.models.Geocache;

import java.util.ArrayList;

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

}
