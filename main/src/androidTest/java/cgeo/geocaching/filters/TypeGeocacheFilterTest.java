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

    @Test
    public void getRawValuesWithPartialSpecialSelection() {
        // Test that getRawValues() correctly expands partial special event selections for API requests
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        
        // Select only MEGA_EVENT and GIGA_EVENT
        filter.setSelectedSpecialEventTypes(new HashSet<>(Arrays.asList(CacheType.MEGA_EVENT, CacheType.GIGA_EVENT)));
        
        // getRawValues should return MEGA_EVENT and GIGA_EVENT, NOT COMMUN_CELEBRATION
        assertThat(filter.getRawValues()).contains(CacheType.MEGA_EVENT, CacheType.GIGA_EVENT);
        assertThat(filter.getRawValues()).doesNotContain(CacheType.COMMUN_CELEBRATION);
        assertThat(filter.getRawValues()).doesNotContain(CacheType.BLOCK_PARTY, CacheType.GPS_EXHIBIT, CacheType.GCHQ_CELEBRATION);
    }

    @Test
    public void getRawValuesWithAllSpecialSelection() {
        // Test that getRawValues() uses COMMUN_CELEBRATION when all special events are selected
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        
        // Select all special events
        filter.setSelectedSpecialEventTypes(TypeGeocacheFilter.getAllSpecialEventTypes());
        
        // getRawValues should return COMMUN_CELEBRATION (the group), not individual types
        assertThat(filter.getRawValues()).contains(CacheType.COMMUN_CELEBRATION);
        // Individual special event types should not be in getRawValues when using the group
        assertThat(filter.getRawValues()).doesNotContain(CacheType.MEGA_EVENT, CacheType.GIGA_EVENT, CacheType.BLOCK_PARTY);
    }

    @Test
    public void getRawValuesWithMixedSelection() {
        // Test that getRawValues() correctly handles mix of regular types and partial special selection
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        
        // Select TRADITIONAL and partial special events (only MEGA_EVENT)
        filter.setValues(new HashSet<>(Collections.singletonList(CacheType.TRADITIONAL)));
        filter.setSelectedSpecialEventTypes(new HashSet<>(Collections.singletonList(CacheType.MEGA_EVENT)));
        
        // getRawValues should return TRADITIONAL and MEGA_EVENT
        assertThat(filter.getRawValues()).contains(CacheType.TRADITIONAL, CacheType.MEGA_EVENT);
        assertThat(filter.getRawValues()).doesNotContain(CacheType.COMMUN_CELEBRATION);
        assertThat(filter.getRawValues()).doesNotContain(CacheType.GIGA_EVENT, CacheType.BLOCK_PARTY);
    }

    @Test
    public void getRawValuesWithPartialSelectionIncludingCommunCelebration() {
        // Test edge case: partial selection that includes COMMUN_CELEBRATION itself
        // COMMUN_CELEBRATION should be excluded from getRawValues to prevent API from expanding it to all special types
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        
        // Select COMMUN_CELEBRATION and MEGA_EVENT (partial selection, not all special events)
        final Set<CacheType> partialSelection = new HashSet<>();
        partialSelection.add(CacheType.COMMUN_CELEBRATION);
        partialSelection.add(CacheType.MEGA_EVENT);
        filter.setSelectedSpecialEventTypes(partialSelection);
        
        // getRawValues should return only MEGA_EVENT, NOT COMMUN_CELEBRATION
        // (to prevent API from expanding COMMUN_CELEBRATION to all special event types)
        assertThat(filter.getRawValues()).contains(CacheType.MEGA_EVENT);
        assertThat(filter.getRawValues()).doesNotContain(CacheType.COMMUN_CELEBRATION);
        assertThat(filter.getRawValues()).doesNotContain(CacheType.GIGA_EVENT, CacheType.BLOCK_PARTY, CacheType.GPS_EXHIBIT);
    }

    @Test
    public void getRawValuesWithOnlyCommunCelebrationSelected() {
        // Test edge case: only COMMUN_CELEBRATION selected (no other special events)
        // In this case, we must send COMMUN_CELEBRATION even though API will expand it,
        // because excluding it would result in no special events being returned
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        
        // Select only COMMUN_CELEBRATION
        final Set<CacheType> onlyCommunCeleb = new HashSet<>();
        onlyCommunCeleb.add(CacheType.COMMUN_CELEBRATION);
        filter.setSelectedSpecialEventTypes(onlyCommunCeleb);
        
        // getRawValues should return COMMUN_CELEBRATION (API will expand to all special types, but that's the best we can do)
        assertThat(filter.getRawValues()).contains(CacheType.COMMUN_CELEBRATION);
        assertThat(filter.getRawValues()).doesNotContain(CacheType.MEGA_EVENT, CacheType.GIGA_EVENT, CacheType.BLOCK_PARTY);
    }

    @Test
    public void filterAPIResultsWhenOnlyCommunCelebrationSelected() {
        // Test that when COMMUN_CELEBRATION is sent to API (which expands to all special events),
        // the filter() method properly filters the results to show only COMMUN_CELEBRATION events
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        
        // Select only COMMUN_CELEBRATION
        final Set<CacheType> onlyCommunCeleb = new HashSet<>();
        onlyCommunCeleb.add(CacheType.COMMUN_CELEBRATION);
        filter.setSelectedSpecialEventTypes(onlyCommunCeleb);
        
        // Verify getRawValues sends COMMUN_CELEBRATION to API
        assertThat(filter.getRawValues()).contains(CacheType.COMMUN_CELEBRATION);
        
        // Simulate API returning all special event types (which happens when COMMUN_CELEBRATION is sent)
        // The filter should accept only COMMUN_CELEBRATION and reject the others
        
        // COMMUN_CELEBRATION cache should match
        final Geocache communCelebCache = new Geocache();
        communCelebCache.setType(CacheType.COMMUN_CELEBRATION);
        assertThat(filter.filter(communCelebCache)).as("COMMUN_CELEBRATION should match").isTrue();
        
        // Other special event types returned by API should NOT match
        final Geocache megaEventCache = new Geocache();
        megaEventCache.setType(CacheType.MEGA_EVENT);
        assertThat(filter.filter(megaEventCache)).as("MEGA_EVENT should be filtered out").isFalse();
        
        final Geocache gigaEventCache = new Geocache();
        gigaEventCache.setType(CacheType.GIGA_EVENT);
        assertThat(filter.filter(gigaEventCache)).as("GIGA_EVENT should be filtered out").isFalse();
        
        final Geocache blockPartyCache = new Geocache();
        blockPartyCache.setType(CacheType.BLOCK_PARTY);
        assertThat(filter.filter(blockPartyCache)).as("BLOCK_PARTY should be filtered out").isFalse();
        
        final Geocache gchqCelebCache = new Geocache();
        gchqCelebCache.setType(CacheType.GCHQ_CELEBRATION);
        assertThat(filter.filter(gchqCelebCache)).as("GCHQ_CELEBRATION should be filtered out").isFalse();
        
        final Geocache gpsExhibitCache = new Geocache();
        gpsExhibitCache.setType(CacheType.GPS_EXHIBIT);
        assertThat(filter.filter(gpsExhibitCache)).as("GPS_EXHIBIT should be filtered out").isFalse();
    }

    @Test
    public void filterAPIResultsWhenPartialSelectionIncludesCommunCelebration() {
        // Test that when COMMUN_CELEBRATION + MEGA_EVENT are selected,
        // the filter properly accepts both (even though COMMUN_CELEBRATION is excluded from API request)
        final TypeGeocacheFilter filter = new TypeGeocacheFilter();
        
        // Select COMMUN_CELEBRATION and MEGA_EVENT
        final Set<CacheType> partialSelection = new HashSet<>();
        partialSelection.add(CacheType.COMMUN_CELEBRATION);
        partialSelection.add(CacheType.MEGA_EVENT);
        filter.setSelectedSpecialEventTypes(partialSelection);
        
        // Verify COMMUN_CELEBRATION is excluded from getRawValues to prevent API expansion
        assertThat(filter.getRawValues()).contains(CacheType.MEGA_EVENT);
        assertThat(filter.getRawValues()).doesNotContain(CacheType.COMMUN_CELEBRATION);
        
        // The filter should accept both COMMUN_CELEBRATION and MEGA_EVENT
        final Geocache communCelebCache = new Geocache();
        communCelebCache.setType(CacheType.COMMUN_CELEBRATION);
        assertThat(filter.filter(communCelebCache)).as("COMMUN_CELEBRATION should match").isTrue();
        
        final Geocache megaEventCache = new Geocache();
        megaEventCache.setType(CacheType.MEGA_EVENT);
        assertThat(filter.filter(megaEventCache)).as("MEGA_EVENT should match").isTrue();
        
        // Other special event types should NOT match
        final Geocache gigaEventCache = new Geocache();
        gigaEventCache.setType(CacheType.GIGA_EVENT);
        assertThat(filter.filter(gigaEventCache)).as("GIGA_EVENT should be filtered out").isFalse();
        
        final Geocache blockPartyCache = new Geocache();
        blockPartyCache.setType(CacheType.BLOCK_PARTY);
        assertThat(filter.filter(blockPartyCache)).as("BLOCK_PARTY should be filtered out").isFalse();
    }

    private void singleType(final Action1<Geocache> cacheSetter, final Action1<TypeGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.TYPE, cacheSetter, filterSetter, expectedResult);
    }

}
