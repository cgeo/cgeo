package cgeo.geocaching;

import cgeo.CGeoTestCase;
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
        final cgCache cache1 = new cgCache();
        cache1.setGeocode("Cache 1");
        final cgCache cache2 = new cgCache();
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
            StoredList list1 = cgData.getList(listId1);
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
            Set<String> geocodes = new HashSet<String>();
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
        final cgCache cache = new cgCache();
        cache.setGeocode(GEOCODE_CACHE);
        cache.setDetailed(true);
        final cgTrackable trackable = new cgTrackable();
        trackable.setLogs(null);
        final List<cgTrackable> inventory = new ArrayList<cgTrackable>();
        inventory.add(trackable);
        cache.setInventory(inventory);

        try {
            cgData.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
            final cgCache loadedCache = cgData.loadCache(GEOCODE_CACHE, LoadFlags.LOAD_ALL_DB_ONLY);
            assertNotNull("Cache was not saved!", loadedCache);
            assertEquals(1, loadedCache.getInventory().size());
        } finally {
            cgData.removeCache(GEOCODE_CACHE, LoadFlags.REMOVE_ALL);
        }
    }

    // Loading logs for an empty geocode should return an empty list, not null!
    public static void testLoadLogsFromEmptyGeocode() {

        List<LogEntry> logs = cgData.loadLogs("");

        assertNotNull("Logs must not be null", logs);
        assertEquals("Logs from empty geocode must be empty", 0, logs.size());
    }
}
