package cgeo.geocaching.connector.gc;

import java.util.Arrays;

import junit.framework.TestCase;

public class GCBaseTest extends TestCase {
    public static void testSplitJSONKey() {
        assertTrue(Arrays.equals(new int[] { 1, 2 }, GCBase.splitJSONKey("(1, 2)")));
        assertTrue(Arrays.equals(new int[] { 12, 34 }, GCBase.splitJSONKey("(12, 34)")));
        assertTrue(Arrays.equals(new int[] { 1234, 56 }, GCBase.splitJSONKey("(1234,56)")));
        assertTrue(Arrays.equals(new int[] { 1234, 567 }, GCBase.splitJSONKey("(1234,  567)")));
    }
}
