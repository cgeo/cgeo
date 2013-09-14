package cgeo.geocaching;

import cgeo.CGeoTestCase;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.list.StoredList;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataStoreTest extends CGeoTestCase {

    public static void testStoredLists() {

        int listId1 = StoredList.STANDARD_LIST_ID;
        int listId2 = StoredList.STANDARD_LIST_ID;

        // create caches
        final Geocache cache1 = new Geocache();
        cache1.setGeocode("Cache 1");
        final Geocache cache2 = new Geocache();
        cache2.setGeocode("Cache 2");
        assertNotNull(cache2);

        try {

            // create lists
            listId1 = DataStore.createList("DataStore Test");
            assertTrue(listId1 > StoredList.STANDARD_LIST_ID);
            listId2 = DataStore.createList("DataStoreTest");
            assertTrue(listId2 > StoredList.STANDARD_LIST_ID);
            assertTrue(DataStore.getLists().size() >= 2);

            cache1.setDetailed(true);
            cache1.setListId(listId1);
            cache2.setDetailed(true);
            cache2.setListId(listId1);

            // save caches to DB (cache1=listId1, cache2=listId1)
            DataStore.saveCache(cache1, LoadFlags.SAVE_ALL);
            DataStore.saveCache(cache2, LoadFlags.SAVE_ALL);
            assertTrue(DataStore.getAllCachesCount() >= 2);

            // rename list (cache1=listId1, cache2=listId1)
            assertEquals(1, DataStore.renameList(listId1, "DataStore Test (renamed)"));

            // get list
            final StoredList list1 = DataStore.getList(listId1);
            assertEquals("DataStore Test (renamed)", list1.title);

            // move to list (cache1=listId2, cache2=listId2)
            DataStore.moveToList(Collections.singletonList(cache1), listId2);
            assertEquals(1, DataStore.getAllStoredCachesCount(CacheType.ALL, listId2));

            // remove list (cache1=listId2, cache2=listId2)
            assertTrue(DataStore.removeList(listId1));

            // mark dropped (cache1=1, cache2=0)
            DataStore.markDropped(Collections.singletonList(cache2));

            // mark stored (cache1=1, cache2=listId2)
            DataStore.moveToList(Collections.singletonList(cache2), listId2);
            assertEquals(2, DataStore.getAllStoredCachesCount(CacheType.ALL, listId2));

            // drop stored (cache1=0, cache2=0)
            DataStore.removeList(listId2);

        } finally {

            // remove caches
            final Set<String> geocodes = new HashSet<String>();
            geocodes.add(cache1.getGeocode());
            geocodes.add(cache2.getGeocode());
            DataStore.removeCaches(geocodes, LoadFlags.REMOVE_ALL);

            // remove list
            DataStore.removeList(listId1);
            DataStore.removeList(listId2);
        }
    }

    // Check that queries don't throw an exception (see issue #1429).
    public static void testLoadWaypoints() {
        final Viewport viewport = new Viewport(new Geopoint(-1, -2), new Geopoint(3, 4));
        DataStore.loadWaypoints(viewport, false, false, CacheType.ALL);
        DataStore.loadWaypoints(viewport, false, true, CacheType.ALL);
        DataStore.loadWaypoints(viewport, true, false, CacheType.ALL);
        DataStore.loadWaypoints(viewport, true, true, CacheType.ALL);
        DataStore.loadWaypoints(viewport, false, false, CacheType.TRADITIONAL);
        DataStore.loadWaypoints(viewport, false, true, CacheType.TRADITIONAL);
        DataStore.loadWaypoints(viewport, true, false, CacheType.TRADITIONAL);
        DataStore.loadWaypoints(viewport, true, true, CacheType.TRADITIONAL);
    }

    // Check that saving a cache and trackable without logs works (see #2199)
    public static void testSaveWithoutLogs() {

        final String GEOCODE_CACHE = "TEST";

        // create cache and trackable
        final Geocache cache = new Geocache();
        cache.setGeocode(GEOCODE_CACHE);
        cache.setDetailed(true);
        final Trackable trackable = new Trackable();
        trackable.setLogs(null);
        final List<Trackable> inventory = new ArrayList<Trackable>();
        inventory.add(trackable);
        cache.setInventory(inventory);

        try {
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
            final Geocache loadedCache = DataStore.loadCache(GEOCODE_CACHE, LoadFlags.LOAD_ALL_DB_ONLY);
            assertNotNull("Cache was not saved!", loadedCache);
            assertEquals(1, loadedCache.getInventory().size());
        } finally {
            DataStore.removeCache(GEOCODE_CACHE, LoadFlags.REMOVE_ALL);
        }
    }

    // Check that loading a cache by case insensitive geo code works correctly (see #3139)
    public static void testGeocodeCaseInsensitive() {

        final String GEOCODE_CACHE = "TEST";
        final String upperCase = GEOCODE_CACHE;
        final String lowerCase = StringUtils.lowerCase(upperCase);
        assertFalse(upperCase.equals(lowerCase));

        // create cache and trackable
        final Geocache cache = new Geocache();
        cache.setGeocode(upperCase);
        cache.setDetailed(true);

        try {
            final Geocache oldCache = DataStore.loadCache(upperCase, LoadFlags.LOAD_ALL_DB_ONLY);
            assertNull("Database contained old cache!", oldCache);

            DataStore.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
            final Geocache cacheWithOriginalCode = DataStore.loadCache(upperCase, LoadFlags.LOAD_ALL_DB_ONLY);
            assertNotNull("Cache was not saved correctly!", cacheWithOriginalCode);

            final Geocache cacheLowerCase = DataStore.loadCache(lowerCase, LoadFlags.LOAD_ALL_DB_ONLY);
            assertNotNull("Could not find cache by case insensitive geocode", cacheLowerCase);

        } finally {
            DataStore.removeCache(upperCase, LoadFlags.REMOVE_ALL);
        }
    }

    // Loading logs for an empty geocode should return an empty list, not null!
    public static void testLoadLogsFromEmptyGeocode() {

        final List<LogEntry> logs = DataStore.loadLogs("");

        assertNotNull("Logs must not be null", logs);
        assertEquals("Logs from empty geocode must be empty", 0, logs.size());
    }

    public static void testLoadCacheHistory() {
        int sumCaches = 0;
        int allCaches = 0;
        for (CacheType cacheType : CacheType.values()) {
            SearchResult historyOfType = DataStore.getHistoryOfCaches(false, cacheType);
            assertNotNull(historyOfType);
            if (cacheType != CacheType.ALL) {
                sumCaches += historyOfType.getCount();
            } else {
                allCaches = historyOfType.getCount();
            }
        }
        // check that sum of types equals 'all'
        assertEquals(sumCaches, allCaches);
        // check that two different routines behave the same
        assertEquals(DataStore.getAllHistoryCachesCount(), sumCaches);
    }

    public static void testCachedMissing() {

        // Tile to test
        final Tile tile = new Tile(new Geopoint("N49 44.0 E8 37.0"), 14);
        final Set<Tile> tiles = new HashSet<Tile>();
        tiles.add(tile);

        // set up geocaches to fill into cacheCache
        final Geocache main = new Geocache();
        main.setGeocode("GC12345");
        main.setCoords(new Geopoint("N49 44.0 E8 37.0"));
        final Geocache inTileLowZoom = new Geocache();
        inTileLowZoom.setGeocode("GC12346");
        inTileLowZoom.setCoords(new Geopoint("N49 44.001 E8 37.001"), Tile.ZOOMLEVEL_MIN_PERSONALIZED - 5);
        final Geocache outTile = new Geocache();
        outTile.setGeocode("GC12347");
        outTile.setCoords(new Geopoint(tile.getViewport().getLatitudeMin() - 0.1, tile.getViewport().getLongitudeMin() - 0.1));
        final Geocache otherConnector = new Geocache();
        otherConnector.setGeocode("OC0001");
        otherConnector.setCoords(new Geopoint("N49 44.0 E8 37.0"));
        final Geocache inTileHighZoom = new Geocache();
        inTileHighZoom.setGeocode("GC12348");
        inTileHighZoom.setCoords(new Geopoint("N49 44.001 E8 37.001"), Tile.ZOOMLEVEL_MIN_PERSONALIZED + 1);

        // put in cache
        DataStore.saveCache(main, EnumSet.of(SaveFlag.SAVE_CACHE));
        DataStore.saveCache(inTileLowZoom, EnumSet.of(SaveFlag.SAVE_CACHE));
        DataStore.saveCache(inTileHighZoom, EnumSet.of(SaveFlag.SAVE_CACHE));
        DataStore.saveCache(outTile, EnumSet.of(SaveFlag.SAVE_CACHE));
        DataStore.saveCache(otherConnector, EnumSet.of(SaveFlag.SAVE_CACHE));

        final SearchResult search = new SearchResult(main);

        Set<String> filteredGeoCodes = DataStore.getCachedMissingFromSearch(search, tiles, GCConnector.getInstance(), Tile.ZOOMLEVEL_MIN_PERSONALIZED - 1);

        assertTrue(filteredGeoCodes.contains(inTileLowZoom.getGeocode()));
        assertFalse(filteredGeoCodes.contains(inTileHighZoom.getGeocode()));
        assertFalse(filteredGeoCodes.contains(otherConnector.getGeocode()));
        assertFalse(filteredGeoCodes.contains(outTile.getGeocode()));
        assertFalse(filteredGeoCodes.contains(main.getGeocode()));

    }
}
