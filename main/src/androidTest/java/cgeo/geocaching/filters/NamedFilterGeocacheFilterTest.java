package cgeo.geocaching.filters;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.NamedFilterGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.functions.Action1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class NamedFilterGeocacheFilterTest {

    private final List<NamedFilter> storage = new ArrayList<>();

    @Before
    public void setUp() {
        storage.clear();
        NamedFilter.resetStorageForTesting(storage);
    }

    @After
    public void tearDown() {
        NamedFilter.resetStorageForTesting(null);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void simple() {
        final NamedFilter tf = createSimpleNamedTypeFilter(1, CacheType.TRADITIONAL);
        NamedFilter.storeAll(Collections.singletonList(tf));
        singleNamedFilter(c -> c.setType(CacheType.TRADITIONAL), f -> f.setNamedFilters(Collections.singletonList(tf)), true);
        singleNamedFilter(c -> c.setType(CacheType.MYSTERY), f -> f.setNamedFilters(Collections.singletonList(tf)), false);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void nonNamedFilter() {
        //when no named filter is configured (id=0), then everything is passed through
        singleNamedFilter(c -> c.setType(CacheType.MYSTERY), f -> f.setNamedFilters(Collections.emptyList()), true);
    }

    @Test
    public void preventEndlessLoopOnNesting() {
        //create a named filter which references itself via id
        final NamedFilterGeocacheFilter filterInside = new NamedFilterGeocacheFilter();
        final GeocacheFilter named = GeocacheFilter.create(true, false, filterInside);
        final NamedFilter selfRef = new NamedFilter("self", named).setId(99);
        filterInside.setNamedFilters(Collections.singletonList(selfRef));
        NamedFilter.storeAll(Collections.singletonList(selfRef));

        //assert that filter executes w/o producing infinite loop and that it doesn't filter any cache
        final Geocache cache = new Geocache();
        cache.setType(CacheType.TRADITIONAL);
        assertThat(named.filter(cache)).isTrue();
    }


    private void singleNamedFilter(final Action1<Geocache> cacheSetter, final Action1<NamedFilterGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.NAMED_FILTER, cacheSetter, filterSetter, expectedResult);
    }

    private NamedFilter createSimpleNamedTypeFilter(final int id, final CacheType ... types) {
        final TypeGeocacheFilter tree = new TypeGeocacheFilter();
        tree.setValues(Arrays.asList(types));
        final GeocacheFilter gf = GeocacheFilter.create(true, false, tree);
        return new NamedFilter("namedFilter_" + id, gf).setId(id);
    }

}

