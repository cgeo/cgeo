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
import static org.assertj.core.api.Assertions.assertThat;

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
        singleType(c -> c.setType(CacheType.MEGA_EVENT), f -> f.setValues(new HashSet<>(Collections.singletonList(CacheType.COMMUN_CELEBRATION))), true);
    }

    @Test
    public void partialSpecialEventSelection() {
        // Test selecting only MEGA_EVENT and GIGA_EVENT
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        filter.setSelectedSpecialEventTypes(new HashSet<>(Arrays.asList(CacheType.MEGA_EVENT, CacheType.GIGA_EVENT)));
        
        // MEGA_EVENT cache should match
        final Geocache megaEventCache = new Geocache();
        megaEventCache.setType(CacheType.MEGA_EVENT);
        assertThat(filter.filter(megaEventCache)).isTrue();
        
        // GIGA_EVENT cache should match
        final Geocache gigaEventCache = new Geocache();
        gigaEventCache.setType(CacheType.GIGA_EVENT);
        assertThat(filter.filter(gigaEventCache)).isTrue();
        
        // Other special event types should NOT match
        final Geocache communCelebCache = new Geocache();
        communCelebCache.setType(CacheType.COMMUN_CELEBRATION);
        assertThat(filter.filter(communCelebCache)).isFalse();
        
        final Geocache blockPartyCache = new Geocache();
        blockPartyCache.setType(CacheType.BLOCK_PARTY);
        assertThat(filter.filter(blockPartyCache)).isFalse();
    }

    @Test
    public void allSpecialEventsSelected() {
        // Test selecting all special events - should use COMMUN_CELEBRATION group
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        filter.setSelectedSpecialEventTypes(TypeGeocacheFilter.getAllSpecialEventTypes());
        
        // All special event types should match
        for (final CacheType type : TypeGeocacheFilter.getAllSpecialEventTypes()) {
            final Geocache cache = new Geocache();
            cache.setType(type);
            assertThat(filter.filter(cache)).as("Type " + type + " should match").isTrue();
        }
        
        // COMMUN_CELEBRATION should be in the values
        assertThat(filter.getValues()).contains(CacheType.COMMUN_CELEBRATION);
    }

    @Test
    public void noSpecialEventsSelected() {
        // Test selecting no special events
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        filter.setSelectedSpecialEventTypes(new HashSet<>());
        
        // No special event types should match
        final Geocache megaEventCache = new Geocache();
        megaEventCache.setType(CacheType.MEGA_EVENT);
        assertThat(filter.filter(megaEventCache)).isFalse();
    }

    private void singleType(final Action1<Geocache> cacheSetter, final Action1<TypeGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.TYPE, cacheSetter, filterSetter, expectedResult);
    }

}
