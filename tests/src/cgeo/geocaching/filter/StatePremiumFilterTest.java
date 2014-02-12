package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;

public class StatePremiumFilterTest extends CGeoTestCase {

    private StateFilterFactory.StatePremiumFilter premiumFilter;
    private Geocache premiumCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        premiumFilter = new StateFilterFactory.StatePremiumFilter();
        premiumCache = new Geocache();
        premiumCache.setPremiumMembersOnly(true);
    }

    public void testAccepts() {
        assertThat(premiumFilter.accepts(premiumCache)).isTrue();
        assertThat(premiumFilter.accepts(new Geocache())).isFalse();
    }

}
