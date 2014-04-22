package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(traditionalFilter.accepts(traditional)).isTrue();
        assertThat(traditionalFilter.accepts(mystery)).isFalse();
    }

    public void testFilter() {
        final ArrayList<Geocache> list = new ArrayList<Geocache>();
        traditionalFilter.filter(list);
        assertThat(list).isEmpty();

        list.add(traditional);
        list.add(mystery);
        assertThat(list).hasSize(2);

        traditionalFilter.filter(list);
        assertThat(list).hasSize(1);
        assertThat(list.contains(traditional)).isTrue();

    }

    public static void testGetAllFilters() {
        final int expectedEntries = CacheType.values().length - 1; // hide "all"
        assertThat(new TypeFilter.Factory().getFilters()).hasSize(expectedEntries);
    }

}
