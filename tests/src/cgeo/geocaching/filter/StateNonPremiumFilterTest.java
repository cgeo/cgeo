package cgeo.geocaching.filter;

import cgeo.CGeoTestCase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.filter.StateFilter.StateNonPremiumFilter;

public class StateNonPremiumFilterTest extends CGeoTestCase {

    private StateFilter.StateNonPremiumFilter nonPremiumFilter;
    private cgCache premiumCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        nonPremiumFilter = new StateNonPremiumFilter();
        premiumCache = new cgCache();
        premiumCache.setPremiumMembersOnly(true);
    }

    public void testAccepts() {
        assertFalse(nonPremiumFilter.accepts(premiumCache));
        assertTrue(nonPremiumFilter.accepts(new cgCache()));
    }

}
