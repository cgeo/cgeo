package cgeo.geocaching;

import cgeo.CGeoTestCase;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class cgDataTest extends CGeoTestCase {

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
            listId1 = cgData.createList("cgData Test");
            assertTrue(listId1 > StoredList.STANDARD_LIST_ID);
            listId2 = cgData.createList("cgDataTest");
            assertTrue(listId2 > StoredList.STANDARD_LIST_ID);
            assertTrue(cgData.getLists().size() >= 2);

            cache1.setDetailed(true);
            cache1.setListId(listId1);
            cache2.setDetailed(true);
            cache2.setListId(listId1);

            // save caches to DB (cache1=listId1, cache2=listId1)
            cgData.saveCache(cache1, LoadFlags.SAVE_ALL);
            cgData.saveCache(cache2, LoadFlags.SAVE_ALL);
            assertTrue(cgData.getAllCachesCount() >= 2);

            // rename list (cache1=listId1, cache2=listId1)
            assertEquals(1, cgData.renameList(listId1, "cgData Test (renamed)"));

            // get list
            final StoredList list1 = cgData.getList(listId1);
            assertEquals("cgData Test (renamed)", list1.title);

            // move to list (cache1=listId2, cache2=listId2)
            cgData.moveToList(Collections.singletonList(cache1), listId2);
            assertEquals(1, cgData.getAllStoredCachesCount(CacheType.ALL, listId2));

            // remove list (cache1=listId2, cache2=listId2)
            assertTrue(cgData.removeList(listId1));

            // mark dropped (cache1=1, cache2=0)
            cgData.markDropped(Collections.singletonList(cache2));

            // mark stored (cache1=1, cache2=listId2)
            cgData.moveToList(Collections.singletonList(cache2), listId2);
            assertEquals(2, cgData.getAllStoredCachesCount(CacheType.ALL, listId2));

            // drop stored (cache1=0, cache2=0)
            cgData.removeList(listId2);

        } finally {

            // remove caches
            final Set<String> geocodes = new HashSet<String>();
            geocodes.add(cache1.getGeocode());
            geocodes.add(cache2.getGeocode());
            cgData.removeCaches(geocodes, LoadFlags.REMOVE_ALL);

            // remove list
            cgData.removeList(listId1);
            cgData.removeList(listId2);
        }
    }

    // Check that queries don't throw an exception (see issue #1429).
    public static void testLoadWaypoints() {
        final Viewport viewport = new Viewport(new Geopoint(-1, -2), new Geopoint(3, 4));
        cgData.loadWaypoints(viewport, false, false, CacheType.ALL);
        cgData.loadWaypoints(viewport, false, true, CacheType.ALL);
        cgData.loadWaypoints(viewport, true, false, CacheType.ALL);
        cgData.loadWaypoints(viewport, true, true, CacheType.ALL);
        cgData.loadWaypoints(viewport, false, false, CacheType.TRADITIONAL);
        cgData.loadWaypoints(viewport, false, true, CacheType.TRADITIONAL);
        cgData.loadWaypoints(viewport, true, false, CacheType.TRADITIONAL);
        cgData.loadWaypoints(viewport, true, true, CacheType.TRADITIONAL);
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
            cgData.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
            final Geocache loadedCache = cgData.loadCache(GEOCODE_CACHE, LoadFlags.LOAD_ALL_DB_ONLY);
            assertNotNull("Cache was not saved!", loadedCache);
            assertEquals(1, loadedCache.getInventory().size());
        } finally {
            cgData.removeCache(GEOCODE_CACHE, LoadFlags.REMOVE_ALL);
        }
    }

    // Loading logs for an empty geocode should return an empty list, not null!
    public static void testLoadLogsFromEmptyGeocode() {

        final List<LogEntry> logs = cgData.loadLogs("");

        assertNotNull("Logs must not be null", logs);
        assertEquals("Logs from empty geocode must be empty", 0, logs.size());
    }

    public static void testLoadCacheHistory() {
        int sumCaches = 0;
        int allCaches = 0;
        for (CacheType cacheType : CacheType.values()) {
            SearchResult historyOfType = cgData.getHistoryOfCaches(false, cacheType);
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
        assertEquals(cgData.getAllHistoryCachesCount(), sumCaches);
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
        final Geocache inTile = new Geocache();
        inTile.setGeocode("GC12346");
        inTile.setCoords(new Geopoint("N49 44.001 E8 37.001"));
        final Geocache outTile = new Geocache();
        outTile.setGeocode("GC12347");
        outTile.setCoords(new Geopoint(tile.getViewport().getLatitudeMin() - 0.1, tile.getViewport().getLongitudeMin() - 0.1));
        final Geocache otherConnector = new Geocache();
        otherConnector.setGeocode("OC0001");
        otherConnector.setCoords(new Geopoint("N49 44.0 E8 37.0"));

        // put in cache
        cgData.saveCache(main, EnumSet.of(SaveFlag.SAVE_CACHE));
        cgData.saveCache(inTile, EnumSet.of(SaveFlag.SAVE_CACHE));
        cgData.saveCache(outTile, EnumSet.of(SaveFlag.SAVE_CACHE));
        cgData.saveCache(otherConnector, EnumSet.of(SaveFlag.SAVE_CACHE));

        final SearchResult search = new SearchResult(main);

        Set<String> filteredGeoCodes = cgData.getCachedMissingFromSearch(search, tiles, GCConnector.getInstance());

        assertTrue(filteredGeoCodes.contains(inTile.getGeocode()));
        assertFalse(filteredGeoCodes.contains(otherConnector.getGeocode()));
        assertFalse(filteredGeoCodes.contains(outTile.getGeocode()));
        assertFalse(filteredGeoCodes.contains(main.getGeocode()));

    }
}
