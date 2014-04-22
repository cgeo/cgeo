package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.filter.StateFilter.StateNotStoredFilter;
import cgeo.geocaching.filter.StateFilter.StateStoredFilter;

public class StateStoredFilterTest extends CGeoTestCase {

    private StateFilter.StateStoredFilter storedFilter;
    private StateFilter.StateNotStoredFilter notStoredFilter;
    private Geocache cache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        storedFilter = new StateStoredFilter();
        notStoredFilter = new StateNotStoredFilter();
        cache = new Geocache();
    }

    public void testAccepts() {
        assertThat(storedFilter.accepts(cache)).isFalse();
        assertThat(notStoredFilter.accepts(cache)).isTrue();
    }

}
