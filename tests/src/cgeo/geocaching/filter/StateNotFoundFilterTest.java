package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.filter.StateFilter.StateNotFoundFilter;

public class StateNotFoundFilterTest extends CGeoTestCase {

    private StateFilter.StateNotFoundFilter notFoundFilter;
    private Geocache foundCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        notFoundFilter = new StateNotFoundFilter();
        foundCache = new Geocache();
        foundCache.setFound(true);
    }

    public void testAccepts() {
        assertThat(notFoundFilter.accepts(foundCache)).isFalse();
        assertThat(notFoundFilter.accepts(new Geocache())).isTrue();
    }

}
