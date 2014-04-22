package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(disabledFilter.accepts(disabledCache)).isTrue();
        assertThat(disabledFilter.accepts(new Geocache())).isFalse();
    }

}
