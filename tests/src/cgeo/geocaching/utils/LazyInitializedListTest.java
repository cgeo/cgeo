package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class LazyInitializedListTest extends TestCase {
    public static void testAccess() {
        final LazyInitializedList<String> list = new LazyInitializedList<String>() {
            @Override
            protected List<String> loadFromDatabase() {
                return new ArrayList<String>();
            }
        };
        assertTrue(list.isEmpty());
        list.add("Test");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        int iterations = 0;
        for (String element : list) {
            assertEquals("Test", element);
            iterations++;
        }
        assertEquals(1, iterations);
    }
}
