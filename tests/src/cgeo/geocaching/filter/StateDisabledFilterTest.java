package cgeo.geocaching.filter;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.filter.StateFilter.StateDisabledFilter;

public class StateDisabledFilterTest extends CGeoTestCase {

    private StateFilter.StateDisabledFilter disabledFilter;
    private Geocache disabledCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        disabledFilter = new StateDisabledFilter();
        disabledCache = new Geocache();
        disabledCache.setDisabled(true);
    }

    public void testAccepts() {
        assertTrue(disabledFilter.accepts(disabledCache));
        assertFalse(disabledFilter.accepts(new Geocache()));
    }

}
