package cgeo.geocaching.filter;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheType;

import java.util.ArrayList;

public class TypeFilterTest extends CGeoTestCase {

    private TypeFilter traditionalFilter;
    private Geocache traditional;
    private Geocache mystery;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        traditionalFilter = new TypeFilter(CacheType.TRADITIONAL);

        traditional = new Geocache();
        traditional.setType(CacheType.TRADITIONAL);

        mystery = new Geocache();
        mystery.setType(CacheType.MYSTERY);
    }

    public void testAccepts() {
        assertTrue(traditionalFilter.accepts(traditional));
        assertFalse(traditionalFilter.accepts(mystery));
    }

    public void testFilter() {
        final ArrayList<Geocache> list = new ArrayList<Geocache>();
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
