package cgeo.geocaching.connector.gc;

import junit.framework.TestCase;

public class GCBaseTest extends TestCase {
    public static void testSplitJSONKey() {
        assertKey("(1, 2)", 1, 2);
        assertKey("(12, 34)", 12, 34);
        assertKey("(1234,56)", 1234, 56);
        assertKey("(1234,  567)", 1234, 567);
    }

    private static void assertKey(String key, int x, int y) {
        UTFGridPosition pos = UTFGridPosition.fromString(key);
        assertEquals(x, pos.getX());
        assertEquals(y, pos.getY());
    }
}
