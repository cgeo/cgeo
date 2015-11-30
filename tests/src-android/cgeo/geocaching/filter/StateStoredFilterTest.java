package cgeo.geocaching.filter;

import junit.framework.TestCase;

import cgeo.geocaching.models.Geocache;

import static org.assertj.core.api.Assertions.assertThat;

public class StateStoredFilterTest extends TestCase {

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
