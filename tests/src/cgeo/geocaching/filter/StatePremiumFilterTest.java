package cgeo.geocaching.filter;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.filter.StateFilter.StatePremiumFilter;

public class StatePremiumFilterTest extends CGeoTestCase {

    private StateFilter.StatePremiumFilter premiumFilter;
    private Geocache premiumCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        premiumFilter = new StatePremiumFilter();
        premiumCache = new Geocache();
        premiumCache.setPremiumMembersOnly(true);
    }

    public void testAccepts() {
        assertTrue(premiumFilter.accepts(premiumCache));
        assertFalse(premiumFilter.accepts(new Geocache()));
    }

}
