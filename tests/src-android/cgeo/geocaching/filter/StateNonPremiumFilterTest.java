package cgeo.geocaching.filter;

import cgeo.geocaching.models.Geocache;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class StateNonPremiumFilterTest extends TestCase {

    private StateFilterFactory.StateNonPremiumFilter nonPremiumFilter;
    private Geocache premiumCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        nonPremiumFilter = new StateFilterFactory.StateNonPremiumFilter();
        premiumCache = new Geocache();
        premiumCache.setPremiumMembersOnly(true);
    }

    public void testAccepts() {
        assertThat(nonPremiumFilter.accepts(premiumCache)).isFalse();
        assertThat(nonPremiumFilter.accepts(new Geocache())).isTrue();
    }

}
