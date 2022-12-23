package cgeo.geocaching.filters;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.functions.Action1;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

public class TypeGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void simpleTradi() {
        singleType(c -> c.setType(CacheType.TRADITIONAL), f -> f.setValues(new HashSet<>(Collections.singletonList(CacheType.TRADITIONAL))), true);
        singleType(c -> c.setType(CacheType.TRADITIONAL), f -> f.setValues(new HashSet<>(Collections.singletonList(CacheType.MULTI))), false);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void allNone() {
        singleType(c -> c.setType(CacheType.TRADITIONAL), null, true);
        singleType(c -> c.setType(CacheType.TRADITIONAL), f -> f.setValues(new HashSet<>(Arrays.asList(CacheType.values()))), true);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void grouping() {
        singleType(c -> c.setType(CacheType.MEGA_EVENT), f -> f.setValues(new HashSet<>(Collections.singletonList(CacheType.EVENT))), true);
    }

    private void singleType(final Action1<Geocache> cacheSetter, final Action1<TypeGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.TYPE, cacheSetter, filterSetter, expectedResult);
    }

}
