package cgeo.geocaching.connector.gc;

import junit.framework.TestCase;

public class UTFGridPositionTest extends TestCase {

    public static void testValidUTFGridPosition() {
        assertNotNull(new UTFGridPosition(0, 0));
    }

    public static void testInvalidUTFGridPosition() {
        boolean valid = true;
        try {
            assertNotNull(new UTFGridPosition(-1, 0));
        } catch (Exception e) {
            valid = false;
        }
        assertFalse(valid);
    }

    public static void testFromString() throws Exception {
        assertXYFromString("(1, 2)", 1, 2);
        assertXYFromString("(12, 34)", 12, 34);
        assertXYFromString("(34,56)", 34, 56);
        assertXYFromString("(34,  56)", 34, 56);
    }

    private static void assertXYFromString(final String key, int x, int y) {
        final UTFGridPosition pos = UTFGridPosition.fromString(key);
        assertEquals(x, pos.getX());
        assertEquals(y, pos.getY());
    }

}
