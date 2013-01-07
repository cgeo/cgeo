package cgeo.geocaching;

import junit.framework.TestCase;

public class StoredListTest extends TestCase {

    public static void testStandardListExists() {
        StoredList list = cgData.getList(StoredList.STANDARD_LIST_ID);
        assertNotNull(list);
    }

    public static void testEquals() {
        StoredList list1 = cgData.getList(StoredList.STANDARD_LIST_ID);
        StoredList list2 = cgData.getList(StoredList.STANDARD_LIST_ID);
        assertEquals(list1, list2);
    }

}
