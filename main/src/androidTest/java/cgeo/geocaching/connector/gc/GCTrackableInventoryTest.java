package cgeo.geocaching.connector.gc;

import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.storage.DataStore;
import static cgeo.geocaching.enumerations.LoadFlags.REMOVE_ALL;

import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GCTrackableInventoryTest {

    private static final String TEST_GEOCODE = "GCINVTEST";
    private static final int TRACKABLES_PER_PAGE = 1000;

    @SmallTest
    @Test
    public void completeInventoryReplacesTravelBugsAndKeepsOtherBrands() {
        final Geocache cache = createCache(createTrackable("TBOLD", TrackableBrand.TRAVELBUG), createTrackable("GK1234", TrackableBrand.GEOKRETY));
        final List<GCWebAPI.TrackableInventoryEntry> inventory = GCWebAPI.collectTrackablesOfCache(
                skip -> createPage(Collections.singletonList(createEntry("TBNEW"))));

        assertThat(inventory).isNotNull();
        GCParser.updateCacheInventory(cache, inventory);

        assertThat(cache.getInventory()).extracting(Trackable::getGeocode).containsExactlyInAnyOrder("TBNEW", "GK1234");
        assertThat(cache.getInventoryItems()).isEqualTo(2);
    }

    @SmallTest
    @Test
    public void completeMultiPageInventoryIsAppliedAfterLastPage() {
        final Geocache cache = createCache(createTrackable("TBOLD", TrackableBrand.TRAVELBUG));
        final List<GCWebAPI.TrackableInventoryEntry> firstPage = new ArrayList<>(TRACKABLES_PER_PAGE);
        for (int i = 0; i < TRACKABLES_PER_PAGE; i++) {
            firstPage.add(createEntry("TB" + i));
        }
        final List<Integer> requestedSkips = new ArrayList<>();
        final List<GCWebAPI.TrackableInventoryEntry> inventory = GCWebAPI.collectTrackablesOfCache(skip -> {
            requestedSkips.add(skip);
            return skip == 0 ? createPage(firstPage) : createPage(Collections.singletonList(createEntry("TBFINAL")));
        });

        assertThat(requestedSkips).containsExactly(0, TRACKABLES_PER_PAGE);
        assertThat(inventory).isNotNull();
        GCParser.updateCacheInventory(cache, inventory);

        assertThat(cache.getInventory()).extracting(Trackable::getGeocode).contains("TB0", "TB999", "TBFINAL").doesNotContain("TBOLD");
        assertThat(cache.getInventoryItems()).isEqualTo(TRACKABLES_PER_PAGE + 1);
    }

    @SmallTest
    @Test
    public void completeEmptyInventoryRemovesTravelBugsAndKeepsOtherBrands() {
        final Geocache cache = createCache(createTrackable("TBOLD", TrackableBrand.TRAVELBUG), createTrackable("GK1234", TrackableBrand.GEOKRETY));
        final List<GCWebAPI.TrackableInventoryEntry> inventory = GCWebAPI.collectTrackablesOfCache(skip -> createPage(Collections.emptyList()));

        assertThat(inventory).isNotNull().isEmpty();
        GCParser.updateCacheInventory(cache, inventory);

        assertThat(cache.getInventory()).extracting(Trackable::getGeocode).containsExactly("GK1234");
        assertThat(cache.getInventoryItems()).isEqualTo(1);
    }

    @SmallTest
    @Test
    public void failedFirstPageKeepsExistingInventory() {
        final Geocache cache = createCache(createTrackable("TBOLD", TrackableBrand.TRAVELBUG), createTrackable("GK1234", TrackableBrand.GEOKRETY));
        final List<GCWebAPI.TrackableInventoryEntry> inventory = GCWebAPI.collectTrackablesOfCache(skip -> {
            throw new IllegalStateException("request failed");
        });

        assertThat(inventory).isNull();
        GCParser.updateCacheInventory(cache, inventory);

        assertThat(cache.getInventory()).extracting(Trackable::getGeocode).containsExactly("TBOLD", "GK1234");
        assertThat(cache.getInventoryItems()).isEqualTo(2);
    }

    @SmallTest
    @Test
    public void missingResponseDataKeepsExistingInventory() {
        final Geocache cache = createCache(createTrackable("TBOLD", TrackableBrand.TRAVELBUG));
        final List<GCWebAPI.TrackableInventoryEntry> inventory = GCWebAPI.collectTrackablesOfCache(
                skip -> new GCWebAPI.TrackableInventoryEntryList());

        assertThat(inventory).isNull();
        GCParser.updateCacheInventory(cache, inventory);

        assertThat(cache.getInventory()).extracting(Trackable::getGeocode).containsExactly("TBOLD");
        assertThat(cache.getInventoryItems()).isEqualTo(1);
    }

    @SmallTest
    @Test
    public void nullEntryKeepsExistingInventory() {
        final Geocache cache = createCache(createTrackable("TBOLD", TrackableBrand.TRAVELBUG));
        final List<GCWebAPI.TrackableInventoryEntry> inventory = GCWebAPI.collectTrackablesOfCache(
                skip -> createPage(Collections.singletonList(null)));

        assertThat(inventory).isNull();
        GCParser.updateCacheInventory(cache, inventory);

        assertThat(cache.getInventory()).extracting(Trackable::getGeocode).containsExactly("TBOLD");
        assertThat(cache.getInventoryItems()).isEqualTo(1);
    }

    @SmallTest
    @Test
    public void missingReferenceCodeKeepsExistingInventory() {
        final Geocache cache = createCache(createTrackable("TBOLD", TrackableBrand.TRAVELBUG));
        final List<GCWebAPI.TrackableInventoryEntry> inventory = GCWebAPI.collectTrackablesOfCache(
                skip -> createPage(Collections.singletonList(createEntry(null))));

        assertThat(inventory).isNull();
        GCParser.updateCacheInventory(cache, inventory);

        assertThat(cache.getInventory()).extracting(Trackable::getGeocode).containsExactly("TBOLD");
        assertThat(cache.getInventoryItems()).isEqualTo(1);
    }

    @SmallTest
    @Test
    public void failedSecondPageDoesNotApplyPartialInventory() {
        final Geocache cache = createCache(createTrackable("TBOLD", TrackableBrand.TRAVELBUG));
        final List<GCWebAPI.TrackableInventoryEntry> firstPage = new ArrayList<>(TRACKABLES_PER_PAGE);
        for (int i = 0; i < TRACKABLES_PER_PAGE; i++) {
            firstPage.add(createEntry("TB" + i));
        }
        final List<Integer> requestedSkips = new ArrayList<>();
        final List<GCWebAPI.TrackableInventoryEntry> inventory = GCWebAPI.collectTrackablesOfCache(skip -> {
            requestedSkips.add(skip);
            if (requestedSkips.size() == 1) {
                return createPage(firstPage);
            }
            throw new IllegalStateException("second page failed");
        });

        assertThat(requestedSkips).containsExactly(0, TRACKABLES_PER_PAGE);
        assertThat(inventory).isNull();
        GCParser.updateCacheInventory(cache, inventory);

        assertThat(cache.getInventory()).extracting(Trackable::getGeocode).containsExactly("TBOLD");
        assertThat(cache.getInventoryItems()).isEqualTo(1);
    }

    @MediumTest
    @Test
    public void completeEmptyInventorySurvivesGatherSaveAndReload() {
        DataStore.removeCache(TEST_GEOCODE, REMOVE_ALL);
        final Geocache storedCache = createCache(createTrackable("TBOLD", TrackableBrand.TRAVELBUG));
        storedCache.setGeocode(TEST_GEOCODE);
        storedCache.setDetailed(true);

        try {
            DataStore.saveCache(storedCache, EnumSet.of(LoadFlags.SaveFlag.DB));
            final Geocache initiallyReloadedCache = DataStore.loadCache(TEST_GEOCODE, LoadFlags.LOAD_ALL_DB_ONLY);
            assertThat(initiallyReloadedCache).isNotNull();
            assertThat(initiallyReloadedCache.getInventory()).extracting(Trackable::getGeocode).containsExactly("TBOLD");
            assertThat(initiallyReloadedCache.getInventoryItems()).isEqualTo(1);

            final Geocache refreshedCache = new Geocache();
            refreshedCache.setGeocode(TEST_GEOCODE);
            refreshedCache.setDetailed(true);
            GCParser.updateCacheInventory(refreshedCache, Collections.emptyList());
            refreshedCache.gatherMissingFrom(storedCache);

            assertThat(refreshedCache.getInventory()).isEmpty();
            assertThat(refreshedCache.getInventoryItems()).isZero();

            DataStore.saveCache(refreshedCache, EnumSet.of(LoadFlags.SaveFlag.DB));
            final Geocache reloadedCache = DataStore.loadCache(TEST_GEOCODE, LoadFlags.LOAD_ALL_DB_ONLY);

            assertThat(reloadedCache).isNotNull();
            assertThat(reloadedCache.getInventory()).isEmpty();
            assertThat(reloadedCache.getInventoryItems()).isZero();
        } finally {
            DataStore.removeCache(TEST_GEOCODE, REMOVE_ALL);
        }
    }

    private static Geocache createCache(final Trackable... trackables) {
        final Geocache cache = new Geocache();
        cache.setInventory(new ArrayList<>(Arrays.asList(trackables)));
        return cache;
    }

    private static Trackable createTrackable(final String geocode, final TrackableBrand brand) {
        final Trackable trackable = new Trackable();
        trackable.setGeocode(geocode);
        trackable.setName(geocode);
        trackable.forceSetBrand(brand);
        return trackable;
    }

    private static GCWebAPI.TrackableInventoryEntry createEntry(final String geocode) {
        final GCWebAPI.TrackableInventoryEntry entry = new GCWebAPI.TrackableInventoryEntry();
        entry.referenceCode = geocode;
        entry.name = geocode;
        entry.trackingNumber = "TRACK-" + geocode;
        return entry;
    }

    private static GCWebAPI.TrackableInventoryEntryList createPage(final List<GCWebAPI.TrackableInventoryEntry> entries) {
        final GCWebAPI.TrackableInventoryEntryList page = new GCWebAPI.TrackableInventoryEntryList();
        page.data = entries;
        page.total = entries.size();
        return page;
    }
}
