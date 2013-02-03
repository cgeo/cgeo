package cgeo.geocaching.filter;

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
        assertTrue(foundFilter.accepts(foundCache));
        assertFalse(foundFilter.accepts(new Geocache()));
    }

}
