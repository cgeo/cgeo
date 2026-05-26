package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.utils.EmojiUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ConditionalCacheMarkerTest {

    private static final int MARKER_A = 0x1f600; // smiley emoji
    private static final int MARKER_B = 0x2764;  // heart emoji

    // ---- getMarkersForRules tests ----

    @Test
    public void getMarkersForRulesEmptyRules() {
        final Geocache cache = new Geocache();
        final List<Integer> result = ConditionalCacheMarker.getMarkersForRules(cache, Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    public void getMarkersForRulesNullCache() {
        final ConditionalCacheMarker rule = new ConditionalCacheMarker(MARKER_A, null);
        final List<Integer> result = ConditionalCacheMarker.getMarkersForRules(null, Collections.singletonList(rule));
        assertThat(result).isEmpty();
    }

    @Test
    public void getMarkersForRulesNullFilterMatchesAll() {
        final Geocache cache = new Geocache();
        final ConditionalCacheMarker rule = new ConditionalCacheMarker(MARKER_A, null);
        final List<Integer> result = ConditionalCacheMarker.getMarkersForRules(cache, Collections.singletonList(rule));
        assertThat(result).containsExactly(MARKER_A);
    }

    @Test
    public void getMarkersForRulesEmptyFilterMatchesAll() {
        // A non-null empty (non-filtering) filter also matches every cache
        final Geocache cache = new Geocache();
        final GeocacheFilter emptyFilter = GeocacheFilter.createEmpty();
        final ConditionalCacheMarker rule = new ConditionalCacheMarker(MARKER_A, emptyFilter);
        final List<Integer> result = ConditionalCacheMarker.getMarkersForRules(cache, Collections.singletonList(rule));
        assertThat(result).containsExactly(MARKER_A);
    }

    @Test
    public void getMarkersForRulesNoMatchingFilterNotIncluded() {
        // TypeGeocacheFilter restricted to TRADITIONAL; cache is EARTH → should not match
        final Geocache cache = new Geocache();
        cache.setType(CacheType.EARTH);

        final TypeGeocacheFilter typeFilter = GeocacheFilterType.TYPE.create();
        typeFilter.setValues(Collections.singleton(CacheType.TRADITIONAL));
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, typeFilter);

        final ConditionalCacheMarker rule = new ConditionalCacheMarker(MARKER_A, filter);
        final List<Integer> result = ConditionalCacheMarker.getMarkersForRules(cache, Collections.singletonList(rule));
        assertThat(result).isEmpty();
    }

    @Test
    public void getMarkersForRulesMatchingFilterIncluded() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.TRADITIONAL);

        final TypeGeocacheFilter typeFilter = GeocacheFilterType.TYPE.create();
        typeFilter.setValues(Collections.singleton(CacheType.TRADITIONAL));
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, typeFilter);

        final ConditionalCacheMarker rule = new ConditionalCacheMarker(MARKER_A, filter);
        final List<Integer> result = ConditionalCacheMarker.getMarkersForRules(cache, Collections.singletonList(rule));
        assertThat(result).containsExactly(MARKER_A);
    }

    @Test
    public void getMarkersForRulesNoEmojiSkipped() {
        final Geocache cache = new Geocache();
        final ConditionalCacheMarker rule = new ConditionalCacheMarker(EmojiUtils.NO_EMOJI, null);
        final List<Integer> result = ConditionalCacheMarker.getMarkersForRules(cache, Collections.singletonList(rule));
        assertThat(result).isEmpty();
    }

    @Test
    public void getMarkersForRulesOrderPreserved() {
        final Geocache cache = new Geocache();
        final ConditionalCacheMarker ruleA = new ConditionalCacheMarker(MARKER_A, null);
        final ConditionalCacheMarker ruleB = new ConditionalCacheMarker(MARKER_B, null);
        final List<Integer> result = ConditionalCacheMarker.getMarkersForRules(
                cache, Arrays.asList(ruleA, ruleB));
        assertThat(result).containsExactly(MARKER_A, MARKER_B);
    }

    // ---- JSON round-trip tests ----

    @Test
    public void jsonRoundTripWithFilter() {
        final TypeGeocacheFilter typeFilter = GeocacheFilterType.TYPE.create();
        typeFilter.setValues(Collections.singleton(CacheType.TRADITIONAL));
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, typeFilter);
        final ConditionalCacheMarker original = new ConditionalCacheMarker(MARKER_A, filter);

        final ConditionalCacheMarker restored = ConditionalCacheMarker.fromJson(original.toJson());

        assertThat(restored.getMarkerId()).isEqualTo(MARKER_A);
        assertThat(restored.getFilter()).isNotNull();
        assertThat(restored.getFilter().toConfig()).isEqualTo(filter.toConfig());
    }

    @Test
    public void jsonRoundTripNullFilter() {
        final ConditionalCacheMarker original = new ConditionalCacheMarker(MARKER_B, null);

        final ConditionalCacheMarker restored = ConditionalCacheMarker.fromJson(original.toJson());

        assertThat(restored.getMarkerId()).isEqualTo(MARKER_B);
        assertThat(restored.getFilter()).isNull();
    }
}


