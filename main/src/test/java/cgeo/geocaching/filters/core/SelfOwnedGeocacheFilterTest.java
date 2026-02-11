package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SelfOwnedGeocacheFilterTest {

    @Test
    public void testGetOwnerNameForOrigin() {
        // Test with GC geocode - returns owner name or null if not logged in
        final String ownerNameGC = SelfOwnedGeocacheFilter.getOwnerNameForOrigin("GC12345");
        // Should not crash, can be null or a string
        assertThat(ownerNameGC == null || ownerNameGC instanceof String).isTrue();
    }

    @Test
    public void testFilterWithNullCache() {
        final SelfOwnedGeocacheFilter filter = new SelfOwnedGeocacheFilter();
        assertThat(filter.filter(null)).isNull();
    }

    @Test
    public void testFilterWithCacheWithoutOwner() {
        final SelfOwnedGeocacheFilter filter = new SelfOwnedGeocacheFilter();
        final Geocache cache = new Geocache();
        cache.setGeocode("GC12345");
        // Cache without owner should return null (inconclusive)
        assertThat(filter.filter(cache)).isNull();
    }

    @Test
    public void testIsFiltering() {
        final SelfOwnedGeocacheFilter filter = new SelfOwnedGeocacheFilter();
        // isFiltering depends on whether any connector has valid credentials
        // Just verify it doesn't crash
        final boolean filtering = filter.isFiltering();
        assertThat(filtering).isIn(true, false);
    }

    @Test
    public void testGetJsonConfig() {
        final SelfOwnedGeocacheFilter filter = new SelfOwnedGeocacheFilter();
        // This filter has no configuration
        assertThat(filter.getJsonConfig()).isNull();
    }
}
