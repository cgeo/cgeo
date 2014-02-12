package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;

public class StateStoredFilterTest extends CGeoTestCase {

    private StateFilterFactory.StateStoredFilter storedFilter;
    private StateFilterFactory.StateNotStoredFilter notStoredFilter;
    private Geocache cache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        storedFilter = new StateFilterFactory.StateStoredFilter();
        notStoredFilter = new StateFilterFactory.StateNotStoredFilter();
        cache = new Geocache();
    }

    public void testAccepts() {
        assertThat(storedFilter.accepts(cache)).isFalse();
        assertThat(notStoredFilter.accepts(cache)).isTrue();
    }

}
