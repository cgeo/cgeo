package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheType;

import android.test.ApplicationTestCase;

import java.util.ArrayList;
import java.util.List;

public class cgDataTest extends ApplicationTestCase<cgeoapplication> {

    public cgDataTest() {
        super(cgeoapplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // init environment
        createApplication();
    }

    public static void testLists() {

        cgeoapplication app = cgeoapplication.getInstance();
        int listId1 = -1;
        int listId2 = -1;

        // create caches
        final cgCache cache1 = cgBaseTest.createCache(0);
        assertNotNull(cache1);
        final cgCache cache2 = cgBaseTest.createCache(1);
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
        SearchResult searchResult = new SearchResult();
        app.addCacheToSearch(searchResult, cache1);
        app.addCacheToSearch(searchResult, cache2);
        assertTrue(app.getAllStoredCachesCount(false, CacheType.ALL, null) >= 2);

        // rename list (cache1=listId1, cache2=listId1)
        assertEquals(1, app.renameList(listId1, "cgData Test (renamed)"));

        // get list
        StoredList list1 = app.getList(listId1);
        assertEquals("cgData Test (renamed)", list1.title);

        // move to list (cache1=listId2, cache2=listId2)
        app.moveToList(cache1.getGeocode(), listId2);
        assertEquals(1, app.getAllStoredCachesCount(false, CacheType.ALL, listId2));

        // remove list (cache1=1, cache2=listId2)
        assertTrue(app.removeList(listId1));

        // mark dropped (cache1=1, cache2=0)
        assertTrue(app.markDropped(cache2.getGeocode()));

        // mark stored (cache1=1, cache2=listId2)
        app.markStored(cache2.getGeocode(), listId2);
        StoredList list2 = app.getList(listId2);
        // assertEquals(1, list2.count);

        // drop stored (cache1=0, cache2=0)
        app.dropStored(listId2);

        } finally {

            // remove caches
            List<String> geocodes = new ArrayList<String>();
            geocodes.add(cache1.getGeocode());
            geocodes.add(cache2.getGeocode());
            app.dropCaches(geocodes);

            // remove list
            app.removeList(listId1);
            app.removeList(listId2);
        }
    }
}