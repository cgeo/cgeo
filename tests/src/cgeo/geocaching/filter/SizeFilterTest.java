package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheSize;

import java.util.ArrayList;

public class SizeFilterTest extends CGeoTestCase {

    private Geocache micro;
    private Geocache regular;
    private SizeFilter microFilter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // cache initialization can only be done without errors after application setup
        micro = new Geocache();
        micro.setSize(CacheSize.MICRO);

        regular = new Geocache();
        regular.setSize(CacheSize.REGULAR);

        microFilter = new SizeFilter(CacheSize.MICRO);
    }

    public void testAccepts() {
        assertThat(microFilter.accepts(micro)).isTrue();
        assertThat(microFilter.accepts(regular)).isFalse();
    }

    public static void testGetAllFilters() {
        final int expectedSizes = CacheSize.values().length - 1; // hide "UNKNOWN"
        assertThat(new SizeFilter.Factory().getFilters()).hasSize(expectedSizes);
    }

    public void testFilter() {
        final ArrayList<Geocache> list = new ArrayList<Geocache>();
        list.add(regular);
        list.add(micro);
        assertThat(list).hasSize(2);

        microFilter.filter(list);
        assertThat(list).hasSize(1);
    }

}
