package cgeo.geocaching.filter;

import cgeo.CGeoTestCase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;

import java.util.ArrayList;

public class TypeFilterTest extends CGeoTestCase {

    private TypeFilter traditionalFilter;
    private cgCache traditional;
    private cgCache mystery;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        traditionalFilter = new TypeFilter(CacheType.TRADITIONAL);

        traditional = new cgCache();
        traditional.setType(CacheType.TRADITIONAL);

        mystery = new cgCache();
        mystery.setType(CacheType.MYSTERY);
    }

    public void testAccepts() {
        assertTrue(traditionalFilter.accepts(traditional));
        assertFalse(traditionalFilter.accepts(mystery));
    }

    public void testFilter() {
        final ArrayList<cgCache> list = new ArrayList<cgCache>();
        traditionalFilter.filter(list);
        assertEquals(0, list.size());

        list.add(traditional);
        list.add(mystery);
        assertEquals(2, list.size());

        traditionalFilter.filter(list);
        assertEquals(1, list.size());
        assertTrue(list.contains(traditional));

    }

    public static void testGetAllFilters() {
        final int expectedEntries = CacheType.values().length - 1; // hide "all"
        assertEquals(expectedEntries, new TypeFilter.Factory().getFilters().length);
    }

}
