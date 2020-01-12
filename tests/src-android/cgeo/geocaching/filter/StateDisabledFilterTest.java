package cgeo.geocaching.filter;

import cgeo.geocaching.models.Geocache;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class StateDisabledFilterTest extends TestCase {

    private StateFilterFactory.StateDisabledFilter disabledFilter;
    private Geocache disabledCache;
    private Geocache archivedCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        disabledFilter = new StateFilterFactory.StateDisabledFilter();
        disabledCache = new Geocache();
        disabledCache.setDisabled(true);

        archivedCache = new Geocache();
        archivedCache.setArchived(true);
        archivedCache.setDisabled(true);
    }

    public void testAccepts() {
        assertThat(disabledFilter.accepts(disabledCache)).isTrue();
        assertThat(disabledFilter.accepts(archivedCache)).isFalse();
        assertThat(disabledFilter.accepts(new Geocache())).isFalse();
    }

}
