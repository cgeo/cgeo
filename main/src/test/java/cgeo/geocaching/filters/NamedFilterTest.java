package cgeo.geocaching.filters;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.NamedFilterGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.EmojiUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class NamedFilterTest {


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
        final NamedFilter nf = new NamedFilter(42, "TestName", filter, 0x1f600, true);

        assertThat(nf.getId()).isEqualTo(42);
        assertThat(nf.getName()).isEqualTo("TestName");
        assertThat(nf.getFilter()).isEqualTo(filter);
        assertThat(nf.getMarkerId()).isEqualTo(0x1f600);
        assertThat(nf.isConditionalMarkerActive()).isTrue();
    }

    @Test
    public void testAddNewAutoIncrementsId() {
        final NamedFilter first = NamedFilter.addNew("First", null);
        final NamedFilter second = NamedFilter.addNew("Second", null);
        final NamedFilter third = NamedFilter.addNew("Third", null);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    public void testAddNewInsertsAtPositionZero() {
        final NamedFilter first = NamedFilter.addNew("First", null);
        final NamedFilter second = NamedFilter.addNew("Second", null);

        // Second added should be at position 0 (highest priority)
        assertThat(NamedFilter.getAll().get(0)).isEqualTo(second);
        assertThat(NamedFilter.getAll().get(1)).isEqualTo(first);
    }

    @Test
    public void testGetAllReturnsUnmodifiableList() {
        NamedFilter.addNew("Test", null);
        final List<NamedFilter> all = NamedFilter.getAll();
        try {
            all.add(new NamedFilter(99, "Extra", null, EmojiUtils.NO_EMOJI, false));
            // If we get here, the list was mutable - fail
            assertThat(false).as("List should be unmodifiable").isTrue();
        } catch (final UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testStoreAllCallsStoreFunction() {

        final List<NamedFilter> newList = Arrays.asList(
                new NamedFilter(1, "A", null, EmojiUtils.NO_EMOJI, false),
                new NamedFilter(2, "B", null, EmojiUtils.NO_EMOJI, false)
        );
        NamedFilter.storeAll(newList);

        assertThat(storage).hasSize(2);
    }

    @Test
    public void testStoreAllReplacesInMemoryList() {
        NamedFilter.addNew("Old", null);
        final List<NamedFilter> newList = Arrays.asList(
                new NamedFilter(10, "NewA", null, EmojiUtils.NO_EMOJI, false),
                new NamedFilter(11, "NewB", null, EmojiUtils.NO_EMOJI, false)
        );
        NamedFilter.storeAll(newList);

        final List<NamedFilter> all = NamedFilter.getAll();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getName()).isEqualTo("NewA");
        assertThat(all.get(1).getName()).isEqualTo("NewB");
    }

    @Test
    public void testGetByIdReturnsCorrectFilter() {
        final NamedFilter nf = new NamedFilter(55, "ByIdTest", null, EmojiUtils.NO_EMOJI, false);
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
                new NamedFilter(1, "Test", null, EmojiUtils.NO_EMOJI, false)));

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

        final NamedFilter nf = new NamedFilter(1, "Tradi", gf, EmojiUtils.NO_EMOJI, false);
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
                new NamedFilter(1, "Tradi", gf, EmojiUtils.NO_EMOJI, false)));

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
        final NamedFilter activeNf = new NamedFilter(1, "Active", activeGf, 0x1f600, true);

        // Passive filter that matches
        final TypeGeocacheFilter typeFilter2 = new TypeGeocacheFilter();
        typeFilter2.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter passiveGf = GeocacheFilter.create(false, false, typeFilter2);
        final NamedFilter passiveNf = new NamedFilter(2, "Passive", passiveGf, 0x2764, false);

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
                new NamedFilter(1, "NoMatch", gf, 0x1f600, true)));

        final ImmutablePair<List<NamedFilter>, List<NamedFilter>> result = NamedFilter.getFiltersMatchingCache(cache);
        assertThat(result.getLeft()).isEmpty();
        assertThat(result.getRight()).isEmpty();
    }

    @Test
    public void testGetFiltersMatchingCacheNullFilterMatchesAll() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.MYSTERY);

        final NamedFilter activeNf = new NamedFilter(1, "AllActive", null, 0x1f600, true);
        final NamedFilter passiveNf = new NamedFilter(2, "AllPassive", null, 0x2764, false);
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

        final NamedFilter activeNf = new NamedFilter(1, "A", gf, 0x1f600, true);
        final NamedFilter passiveNf = new NamedFilter(2, "P", gf, 0x2764, false);
        NamedFilter.storeAll(Arrays.asList(activeNf, passiveNf));

        final List<Integer> markers = NamedFilter.getMarkersForCache(cache);
        assertThat(markers).containsExactly(0x1f600);
    }

    @Test
    public void testToJsonFromJsonRoundTrip() {
        final TypeGeocacheFilter tf = new TypeGeocacheFilter();
        tf.setValues(Collections.singletonList(CacheType.TRADITIONAL));
        final GeocacheFilter gf = GeocacheFilter.create(false, false, tf);

        final NamedFilter original = new NamedFilter(77, "RoundTrip", gf, 0x1f600, true);
        final JsonNode json = original.toJson();
        final NamedFilter restored = NamedFilter.fromJson(json);

        assertThat(restored.getId()).isEqualTo(77);
        assertThat(restored.getName()).isEqualTo("RoundTrip");
        assertThat(restored.getMarkerId()).isEqualTo(0x1f600);
        assertThat(restored.isConditionalMarkerActive()).isTrue();
        assertThat(restored.getFilter()).isNotNull();
    }

    @Test
    public void testFromJsonUsesStoredId() {
        final NamedFilter original = new NamedFilter(123, "StoredId", null, EmojiUtils.NO_EMOJI, false);
        final JsonNode json = original.toJson();
        final NamedFilter restored = NamedFilter.fromJson(json);

        assertThat(restored.getId()).isEqualTo(123);
    }

    @Test
    public void testNoEndlessLoopOnSelfReference() {
        // Create a NamedFilterGeocacheFilter that references itself
        final NamedFilterGeocacheFilter selfRef = new NamedFilterGeocacheFilter();

        final GeocacheFilter gf = GeocacheFilter.create(false, false, selfRef);
        final NamedFilter nf = new NamedFilter(999, "Self", gf, EmojiUtils.NO_EMOJI, true);
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
        final NamedFilter a = new NamedFilter(1, "A", null, EmojiUtils.NO_EMOJI, false);
        final NamedFilter b = new NamedFilter(2, "B", null, EmojiUtils.NO_EMOJI, false);
        final NamedFilter c = new NamedFilter(3, "C", null, EmojiUtils.NO_EMOJI, false);
        NamedFilter.storeAll(Arrays.asList(a, b, c));

        final List<NamedFilter> all = NamedFilter.getAll();
        assertThat(all.get(0).getName()).isEqualTo("A");
        assertThat(all.get(1).getName()).isEqualTo("B");
        assertThat(all.get(2).getName()).isEqualTo("C");
    }

    @Test
    public void testAtomicIdGenerationThreadsafe() throws InterruptedException {
        final int threadCount = 20;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final List<Integer> ids = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                final NamedFilter nf = NamedFilter.addNew("Thread", null);
                ids.add(nf.getId());
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();

        // All IDs must be unique
        assertThat(ids.stream().distinct().collect(Collectors.toList())).hasSize(threadCount);
    }
}
