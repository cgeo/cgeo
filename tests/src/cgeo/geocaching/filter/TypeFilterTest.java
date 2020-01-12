package cgeo.geocaching.filter;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class TypeFilterTest {

    private TypeFilter traditionalFilter;
    private Geocache traditional;
    private Geocache mystery;

    @Before
    public void setUp() throws Exception {
        traditionalFilter = new TypeFilter(CacheType.TRADITIONAL);

        traditional = new Geocache();
        traditional.setType(CacheType.TRADITIONAL);

        mystery = new Geocache();
        mystery.setType(CacheType.MYSTERY);
    }

    @Test
    public void testAccepts() {
        assertThat(traditionalFilter.accepts(traditional)).isTrue();
        assertThat(traditionalFilter.accepts(mystery)).isFalse();
    }

    @Test
    public void testFilter() {
        final ArrayList<Geocache> list = new ArrayList<>();
        traditionalFilter.filter(list);
        assertThat(list).isEmpty();

        list.add(traditional);
        list.add(mystery);
        assertThat(list).hasSize(2);

        traditionalFilter.filter(list);
        assertThat(list).containsExactly(traditional);

    }

    @Test
    public void testGetAllFilters() {
        final int expectedEntries = CacheType.values().length - 1; // hide "all"
        assertThat(new TypeFilter.Factory().getFilters()).hasSize(expectedEntries);
    }

}
