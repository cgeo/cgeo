package cgeo.geocaching.filter;

import cgeo.CGeoTestCase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.filter.StateFilter.StatePremiumFilter;

public class StatePremiumFilterTest extends CGeoTestCase {

    private StateFilter.StatePremiumFilter premiumFilter;
    private cgCache premiumCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        premiumFilter = new StatePremiumFilter();
        premiumCache = new cgCache();
        premiumCache.setPremiumMembersOnly(true);
    }

    public void testAccepts() {
        assertTrue(premiumFilter.accepts(premiumCache));
        assertFalse(premiumFilter.accepts(new cgCache()));
    }

}
