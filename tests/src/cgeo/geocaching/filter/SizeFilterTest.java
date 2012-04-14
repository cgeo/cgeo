package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheSize;

import java.util.ArrayList;

public class SizeFilterTest extends AbstractFilterTestCase {

    private cgCache micro;
    private cgCache regular;
    private SizeFilter microFilter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // cache initialization can only be done without errors after application setup
        micro = new cgCache();
        micro.setSize(CacheSize.MICRO);

        regular = new cgCache();
        regular.setSize(CacheSize.REGULAR);

        microFilter = new SizeFilter(CacheSize.MICRO);
    }

    public void testAccepts() {
        assertTrue(microFilter.accepts(micro));
        assertFalse(microFilter.accepts(regular));
    }

    public static void testGetAllFilters() {
        final int expectedSizes = CacheSize.values().length - 1; // hide "UNKNOWN"
        assertEquals(expectedSizes, new SizeFilter.Factory().getFilters().length);
    }

    public void testFilter() {
        final ArrayList<cgCache> list = new ArrayList<cgCache>();
        list.add(regular);
        list.add(micro);
        assertEquals(2, list.size());

        microFilter.filter(list);
        assertEquals(1, list.size());
    }

}
