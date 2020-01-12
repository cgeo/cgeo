package cgeo.geocaching.filter;

import cgeo.geocaching.models.Geocache;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class StateFoundFilterTest extends TestCase {

    private StateFilterFactory.StateFoundFilter foundFilter;
    private Geocache foundCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        foundFilter = new StateFilterFactory.StateFoundFilter();
        foundCache = new Geocache();
        foundCache.setFound(true);
    }

    public void testAccepts() {
        assertThat(foundFilter.accepts(foundCache)).isTrue();
        assertThat(foundFilter.accepts(new Geocache())).isFalse();
    }

}
