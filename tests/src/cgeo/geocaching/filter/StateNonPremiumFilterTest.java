package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.filter.StateFilter.StateNonPremiumFilter;

public class StateNonPremiumFilterTest extends CGeoTestCase {

    private StateFilter.StateNonPremiumFilter nonPremiumFilter;
    private Geocache premiumCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        nonPremiumFilter = new StateNonPremiumFilter();
        premiumCache = new Geocache();
        premiumCache.setPremiumMembersOnly(true);
    }

    public void testAccepts() {
        assertThat(nonPremiumFilter.accepts(premiumCache)).isFalse();
        assertThat(nonPremiumFilter.accepts(new Geocache())).isTrue();
    }

}
