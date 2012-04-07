package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.filter.StateFilter.StateFoundFilter;

public class StateFoundFilterTest extends AbstractFilterTestCase {

    private StateFilter.StateFoundFilter foundFilter;
    private cgCache foundCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        foundFilter = new StateFoundFilter();
        foundCache = new cgCache();
        foundCache.setFound(true);
    }

    public void testAccepts() {
        assertTrue(foundFilter.accepts(foundCache));
        assertFalse(foundFilter.accepts(new cgCache()));
    }

}
