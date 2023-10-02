package cgeo.geocaching.filters;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.NamedFilterGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.functions.Action1;

import java.util.Arrays;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class NamedFilterGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void simple() {
        singleNamedFilter(c -> c.setType(CacheType.TRADITIONAL), f -> f.setNamedFilter(createSimpleNamedTypeFilter("test", CacheType.TRADITIONAL)), true);
        singleNamedFilter(c -> c.setType(CacheType.MYSTERY), f -> f.setNamedFilter(createSimpleNamedTypeFilter("test", CacheType.TRADITIONAL)), false);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void nonNamedFilter() {
        //when no named filter is configured, then everything is passed through
        singleNamedFilter(c -> c.setType(CacheType.MYSTERY), f -> f.setNamedFilter(createSimpleNamedTypeFilter(null, CacheType.TRADITIONAL)), true);
    }

    @Test
    public void preventEndlessLoopOnNesting() {
        //create a named filter which references itself
        final NamedFilterGeocacheFilter filterInside = new NamedFilterGeocacheFilter();
        final GeocacheFilter named = GeocacheFilter.create("named", true, false, filterInside);
        filterInside.setNamedFilter(named);

        //assert that filter executes w/o producing infinite loop and that it doesn't filter any cache
        final Geocache cache = new Geocache();
        cache.setType(CacheType.TRADITIONAL);
        assertThat(named.filter(cache)).isTrue();
    }


    private void singleNamedFilter(final Action1<Geocache> cacheSetter, final Action1<NamedFilterGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.NAMED_FILTER, cacheSetter, filterSetter, expectedResult);
    }

    private GeocacheFilter createSimpleNamedTypeFilter(final String name, final CacheType ... types) {
        final TypeGeocacheFilter tree = new TypeGeocacheFilter();
        tree.setValues(Arrays.asList(types));
        return GeocacheFilter.create(name, true, false, tree);
    }

}
