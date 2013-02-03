package cgeo.geocaching.utils;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class LazyInitializedListTest extends TestCase {

    private static final class MockedLazyInitializedList extends LazyInitializedList<String> {
        @Override
        public List<String> call() {
            return new ArrayList<String>();
        }
    }

    public static void testAccess() {
        final LazyInitializedList<String> list = new MockedLazyInitializedList();
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
