package cgeo.geocaching.filter;

import cgeo.geocaching.models.Geocache;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class StateStoredFilterTest extends TestCase {

    private StoredFilterFactory.StateStoredFilter storedFilter;
    private StoredFilterFactory.StateNotStoredFilter notStoredFilter;
    private Geocache cache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        storedFilter = new StoredFilterFactory.StateStoredFilter();
        notStoredFilter = new StoredFilterFactory.StateNotStoredFilter();
        cache = new Geocache();
    }

    public void testAccepts() {
        assertThat(storedFilter.accepts(cache)).isFalse();
        assertThat(notStoredFilter.accepts(cache)).isTrue();
    }

}
