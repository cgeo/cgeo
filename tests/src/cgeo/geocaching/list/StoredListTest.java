package cgeo.geocaching.list;

import cgeo.geocaching.DataStore;

import junit.framework.TestCase;

public class StoredListTest extends TestCase {

    public static void testStandardListExists() {
        final StoredList list = getStandardList();
        assertNotNull(list);
    }

    private static StoredList getStandardList() {
        return DataStore.getList(StoredList.STANDARD_LIST_ID);
    }

    public static void testEquals() {
        final StoredList list1 = getStandardList();
        final StoredList list2 = getStandardList();
        assertEquals(list1, list2);
    }

    public static void testConcrete() {
        assertTrue(getStandardList().isConcrete());
    }
}
