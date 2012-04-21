package cgeo.geocaching;

import cgeo.CGeoTestCase;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;

import java.util.HashSet;
import java.util.Set;

public class cgDataTest extends CGeoTestCase {

    public static void testStoredLists() {

        cgeoapplication app = cgeoapplication.getInstance();
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
            listId1 = app.createList("cgData Test");
            assertTrue(listId1 > StoredList.STANDARD_LIST_ID);
            listId2 = app.createList("cgDataTest");
            assertTrue(listId2 > StoredList.STANDARD_LIST_ID);
            assertTrue(app.getLists().size() >= 2);

            cache1.setDetailed(true);
            cache1.setListId(listId1);
            cache2.setDetailed(true);
            cache2.setListId(listId1);

            // save caches to DB (cache1=listId1, cache2=listId1)
            app.saveCache(cache1, LoadFlags.SAVE_ALL);
            app.saveCache(cache2, LoadFlags.SAVE_ALL);
            assertTrue(app.getAllStoredCachesCount(false, CacheType.ALL) >= 2);

            // rename list (cache1=listId1, cache2=listId1)
            assertEquals(1, app.renameList(listId1, "cgData Test (renamed)"));

            // get list
            StoredList list1 = app.getList(listId1);
            assertEquals("cgData Test (renamed)", list1.title);

            // move to list (cache1=listId2, cache2=listId2)
            app.moveToList(cache1.getGeocode(), listId2);
            assertEquals(1, app.getAllStoredCachesCount(false, CacheType.ALL, listId2));

            // remove list (cache1=listId2, cache2=listId2)
            assertTrue(app.removeList(listId1));

            // mark dropped (cache1=1, cache2=0)
            app.markDropped(cache2.getGeocode());

            // mark stored (cache1=1, cache2=listId2)
            app.markStored(cache2.getGeocode(), listId2);
            assertEquals(2, app.getAllStoredCachesCount(false, CacheType.ALL, listId2));

            // drop stored (cache1=0, cache2=0)
            app.dropList(listId2);

        } finally {

            // remove caches
            Set<String> geocodes = new HashSet<String>();
            geocodes.add(cache1.getGeocode());
            geocodes.add(cache2.getGeocode());
            app.removeCaches(geocodes, LoadFlags.REMOVE_ALL);

            // remove list
            app.removeList(listId1);
            app.removeList(listId2);
        }
    }

    // Check that queries don't throw an exception (see issue #1429).
    public static void testLoadWaypoints() {
        final Viewport viewport = new Viewport(new Geopoint(-1, -2), new Geopoint(3, 4));
        final cgeoapplication app = cgeoapplication.getInstance();
        app.getWaypointsInViewport(viewport, false, false);
        app.getWaypointsInViewport(viewport, false, true);
        app.getWaypointsInViewport(viewport, true, false);
        app.getWaypointsInViewport(viewport, true, true);
    }
}
