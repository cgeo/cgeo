package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.filter.StateFilter.StateArchivedFilter;

public class StateArchivedFilterTest extends CGeoTestCase {

    private StateFilter.StateArchivedFilter archivedFilter;
    private Geocache archivedCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // members can only be setup here, after application is initialized
        archivedFilter = new StateArchivedFilter();
        archivedCache = new Geocache();
        archivedCache.setArchived(true);
    }

    public void testAccepts() {
        assertThat(archivedFilter.accepts(archivedCache)).isTrue();
        assertThat(archivedFilter.accepts(new Geocache())).isFalse();
    }

}
