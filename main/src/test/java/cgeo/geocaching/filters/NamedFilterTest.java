package cgeo.geocaching.filters;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.NamedFilterGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.EmojiUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class NamedFilterTest {

    private static final String EMOJI_SMILEY = new String(Character.toChars(0x1f600));
    private static final String EMOJI_HEART = new String(Character.toChars(0x2764));

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
    public void testPublicConstructorStoresAllFields() {
        final GeocacheFilter filter = GeocacheFilter.create(false, false, null);
        final NamedFilter nf = new NamedFilter("TestName", filter, EMOJI_SMILEY, true, NamedFilter.MarkerPriority.NORMAL).setId(42);

        assertThat(nf.getId()).isEqualTo(42);
        assertThat(nf.getName()).isEqualTo("TestName");
        assertThat(nf.getFilter()).isEqualTo(filter);
        assertThat(nf.getMarkerId()).isEqualTo(EMOJI_SMILEY);
        assertThat(nf.isConditionalMarkerActive()).isTrue();
    }

    @Test
    public void testAddNewSortsAlphabetically() {
        final NamedFilter n1 = NamedFilter.addNew("B - Second", GeocacheFilter.createEmpty());
        final NamedFilter n2 = NamedFilter.addNew("A - First", GeocacheFilter.createEmpty());
        final NamedFilter n3 = NamedFilter.addNew("C - Third", GeocacheFilter.createEmpty());

        // Second added should be at position 0 (highest priority)
        assertThat(NamedFilter.getAll().get(0).toConfig()).isEqualTo(n2.toConfig());
        assertThat(NamedFilter.getAll().get(1).toConfig()).isEqualTo(n1.toConfig());
        assertThat(NamedFilter.getAll().get(2).toConfig()).isEqualTo(n3.toConfig());
    }

    @Test
    public void testGetAllReturnsUnmodifiableList() {
        NamedFilter.addNew("Test", null);
        final List<NamedFilter> all = NamedFilter.getAll();
        try {
            all.add(new NamedFilter("Extra", null).setId(99));
            // If we get here, the list was mutable - fail
            assertThat(false).as("List should be unmodifiable").isTrue();
        } catch (final UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testStoreAllCallsStoreFunction() {

        final List<NamedFilter> newList = Arrays.asList(
                new NamedFilter("A", null).setId(1),
                new NamedFilter("B", null).setId(2)
        );
        NamedFilter.storeAll(newList);

        assertThat(storage).hasSize(2);
    }

    @Test
    public void testStoreAllReplacesInMemoryList() {
        NamedFilter.addNew("Old", null);
        final List<NamedFilter> newList = Arrays.asList(
                new NamedFilter("NewA", null).setId(10),
                new NamedFilter("NewB", null).setId(11)
        );
        NamedFilter.storeAll(newList);

        final List<NamedFilter> all = NamedFilter.getAll();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getName()).isEqualTo("NewA");
        assertThat(all.get(1).getName()).isEqualTo("NewB");
    }

    @Test
    public void testGetByIdReturnsCorrectFilter() {
        final NamedFilter nf = new NamedFilter("ByIdTest", null).setId(55);
        NamedFilter.storeAll(Collections.singletonList(nf));

        assertThat(NamedFilter.getById(55)).isEqualTo(nf);
    }

    @Test
    public void testGetByIdReturnsNullForUnknownId() {
        assertThat(NamedFilter.getById(999)).isNull();
    }

    @Test
    public void testNameExistsCaseInsensitive() {
        NamedFilter.storeAll(Collections.singletonList(
                new NamedFilter("Test", null).setId(1)));

        assertThat(NamedFilter.nameExists("test")).isTrue();
        assertThat(NamedFilter.nameExists("TEST")).isTrue();
        assertThat(NamedFilter.nameExists("Test")).isTrue();
        assertThat(NamedFilter.nameExists("other")).isFalse();
    }

    @Test
    public void testFilterConfigExistsMatchesEquivalentFilter() {
        final TypeGeocacheFilter typeFilter = new TypeGeocacheFilter();
        typeFilter.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter gf = GeocacheFilter.create(false, false, typeFilter);

        final NamedFilter nf = new NamedFilter("Tradi", gf).setId(1);
        NamedFilter.storeAll(Collections.singletonList(nf));

        // Create an equivalent filter
        final TypeGeocacheFilter typeFilter2 = new TypeGeocacheFilter();
        typeFilter2.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter gf2 = GeocacheFilter.create(false, false, typeFilter2);

        assertThat(NamedFilter.filterConfigExists(gf2)).isEqualTo(nf);
    }

    @Test
    public void testFilterConfigExistsReturnsNullForNoMatch() {
        final TypeGeocacheFilter typeFilter = new TypeGeocacheFilter();
        typeFilter.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter gf = GeocacheFilter.create(false, false, typeFilter);
        NamedFilter.storeAll(Collections.singletonList(
                new NamedFilter("Tradi", gf).setId(1)));

        final TypeGeocacheFilter typeFilter2 = new TypeGeocacheFilter();
        typeFilter2.setValues(Collections.singletonList(CacheType.MYSTERY));
        final GeocacheFilter gf2 = GeocacheFilter.create(false, false, typeFilter2);

        assertThat(NamedFilter.filterConfigExists(gf2)).isNull();
    }

    @Test
    public void testGetFiltersMatchingCacheActiveSeparatedFromPassive() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.TRADITIONAL);

        // Active filter that matches
        final TypeGeocacheFilter typeFilter = new TypeGeocacheFilter();
        typeFilter.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter activeGf = GeocacheFilter.create(false, false, typeFilter);
        final NamedFilter activeNf = new NamedFilter("Active", activeGf, EMOJI_SMILEY, true, NamedFilter.MarkerPriority.NORMAL).setId(1);

        // Passive filter that matches
        final TypeGeocacheFilter typeFilter2 = new TypeGeocacheFilter();
        typeFilter2.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter passiveGf = GeocacheFilter.create(false, false, typeFilter2);
        final NamedFilter passiveNf = new NamedFilter("Passive", passiveGf, EMOJI_HEART, false, NamedFilter.MarkerPriority.NORMAL).setId(2);

        NamedFilter.storeAll(Arrays.asList(activeNf, passiveNf));

        final ImmutablePair<List<NamedFilter>, List<NamedFilter>> result = NamedFilter.getFiltersMatchingCache(cache);
        assertThat(result.getLeft()).containsExactly(activeNf);
        assertThat(result.getRight()).containsExactly(passiveNf);
    }

    @Test
    public void testGetFiltersMatchingCacheNonMatchingNotInResult() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.MYSTERY);

        final TypeGeocacheFilter typeFilter = new TypeGeocacheFilter();
        typeFilter.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter gf = GeocacheFilter.create(false, false, typeFilter);
        NamedFilter.storeAll(Collections.singletonList(
                new NamedFilter("NoMatch", gf, EMOJI_SMILEY, true, null).setId(1)));

        final ImmutablePair<List<NamedFilter>, List<NamedFilter>> result = NamedFilter.getFiltersMatchingCache(cache);
        assertThat(result.getLeft()).isEmpty();
        assertThat(result.getRight()).isEmpty();
    }

    @Test
    public void testGetFiltersMatchingCacheNullFilterMatchesAll() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.MYSTERY);

        final NamedFilter activeNf = new NamedFilter("AllActive", null, EMOJI_SMILEY, true, null).setId(1);
        final NamedFilter passiveNf = new NamedFilter("AllPassive", null, EMOJI_HEART, false, null).setId(2);
        NamedFilter.storeAll(Arrays.asList(activeNf, passiveNf));

        final ImmutablePair<List<NamedFilter>, List<NamedFilter>> result = NamedFilter.getFiltersMatchingCache(cache);
        assertThat(result.getLeft()).containsExactly(activeNf);
        assertThat(result.getRight()).containsExactly(passiveNf);
    }

    @Test
    public void testGetFiltersMatchingCacheNullCacheReturnsEmptyPair() {
        NamedFilter.addNew("Test", null);
        final ImmutablePair<List<NamedFilter>, List<NamedFilter>> result = NamedFilter.getFiltersMatchingCache(null);
        assertThat(result.getLeft()).isEmpty();
        assertThat(result.getRight()).isEmpty();
    }

    @Test
    public void testGetMarkersForCacheUsesActiveFromPair() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.TRADITIONAL);

        final TypeGeocacheFilter tf = new TypeGeocacheFilter();
        tf.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter gf = GeocacheFilter.create(false, false, tf);

        final NamedFilter activeNf = new NamedFilter("A", gf, EMOJI_SMILEY, true, null).setId(1);
        final NamedFilter passiveNf = new NamedFilter("P", gf, EMOJI_HEART, false, null).setId(2);
        NamedFilter.storeAll(Arrays.asList(activeNf, passiveNf));

        final List<String> markers = NamedFilter.getMarkersForCache(cache);
        assertThat(markers).containsExactly(EMOJI_SMILEY);
    }

    @Test
    public void testToJsonFromJsonRoundTrip() {
        final TypeGeocacheFilter tf = GeocacheFilterType.TYPE.create();
        tf.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter gf = GeocacheFilter.create(false, false, tf);

        final NamedFilter original = new NamedFilter("RoundTrip", gf, EMOJI_SMILEY, true, null).setId(77);
        final String config = original.toConfig();
        final NamedFilter restored = NamedFilter.createFromConfig(config);

        assertThat(restored.getId()).isEqualTo(77);
        assertThat(restored.getName()).isEqualTo("RoundTrip");
        assertThat(restored.getMarkerId()).isEqualTo(EMOJI_SMILEY);
        assertThat(restored.isConditionalMarkerActive()).isTrue();
        assertThat(restored.getFilter()).isNotNull();
        assertThat(restored.getFilter().getTree()).isInstanceOf(TypeGeocacheFilter.class);
        assertThat(((TypeGeocacheFilter) restored.getFilter().getTree()).getType()).isEqualTo(GeocacheFilterType.TYPE);
        assertThat(((TypeGeocacheFilter) restored.getFilter().getTree()).getValues()).containsExactly(CacheType.TRADITIONAL);
    }

    @Test
    public void testFromJsonUsesStoredId() {
        final NamedFilter original = new NamedFilter("StoredId", null, EmojiUtils.NO_EMOJI, false, null).setId(123);
        final NamedFilter restored = NamedFilter.createFromConfig(original.toConfig());

        assertThat(restored.getId()).isEqualTo(123);
    }

    @Test
    public void testNoEndlessLoopOnSelfReference() {
        // Create a NamedFilterGeocacheFilter that references itself
        final NamedFilterGeocacheFilter selfRef = new NamedFilterGeocacheFilter();

        final GeocacheFilter gf = GeocacheFilter.create(false, false, selfRef);
        final NamedFilter nf = new NamedFilter("Self", gf, EmojiUtils.NO_EMOJI, true, null).setId(999);
        selfRef.setNamedFilters(Collections.singletonList(nf));
        NamedFilter.storeAll(Collections.singletonList(nf));

        // This should not throw or hang
        final Geocache cache = new Geocache();
        final ImmutablePair<List<NamedFilter>, List<NamedFilter>> result = NamedFilter.getFiltersMatchingCache(cache);
        // No specific assertion about result content — just verify no infinite loop
        assertThat(result).isNotNull();
    }

    @Test
    public void testPriorityOrderPreservedAfterStoreAll() {
        final NamedFilter a = new NamedFilter("A", null).setId(1);
        final NamedFilter b = new NamedFilter("B", null).setId(2);
        final NamedFilter c = new NamedFilter("C", null).setId(3);
        NamedFilter.storeAll(Arrays.asList(a, b, c));

        final List<NamedFilter> all = NamedFilter.getAll();
        assertThat(all.get(0).getName()).isEqualTo("A");
        assertThat(all.get(1).getName()).isEqualTo("B");
        assertThat(all.get(2).getName()).isEqualTo("C");
    }


}
