package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.filter.StateFilter.StateFoundFilter;

public class StateFoundFilterTest extends CGeoTestCase {

    private StateFilter.StateFoundFilter foundFilter;
    private Geocache foundCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        foundFilter = new StateFoundFilter();
        foundCache = new Geocache();
        foundCache.setFound(true);
    }

    public void testAccepts() {
        assertThat(foundFilter.accepts(foundCache)).isTrue();
        assertThat(foundFilter.accepts(new Geocache())).isFalse();
    }

}
