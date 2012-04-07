package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.filter.StateFilter.StateArchivedFilter;

public class StateArchivedFilterTest extends AbstractFilterTestCase {

    private StateFilter.StateArchivedFilter archivedFilter;
    private cgCache archivedCache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // members can only be setup here, after application is initialized
        archivedFilter = new StateArchivedFilter();
        archivedCache = new cgCache();
        archivedCache.setArchived(true);
    }

    public void testAccepts() {
        assertTrue(archivedFilter.accepts(archivedCache));
        assertFalse(archivedFilter.accepts(new cgCache()));
    }

}
