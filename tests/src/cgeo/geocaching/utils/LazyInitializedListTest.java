package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class LazyInitializedListTest extends TestCase {

    private static final class MockedLazyInitializedList extends LazyInitializedList<String> {
        @Override
        protected List<String> loadFromDatabase() {
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

    public static void testNull() {
        final LazyInitializedList<String> list = new MockedLazyInitializedList();
        list.set((LazyInitializedList<String>) null);
        list.set((ArrayList<String>) null);
    }

    public static void testUnmodifiable() {
        final MockedLazyInitializedList list = new MockedLazyInitializedList();
        boolean unsupported = false;
        try {
            list.asList().add("this is not possible");
        } catch (UnsupportedOperationException e) {
            unsupported = true;
        }
        assertTrue(unsupported);
    }
}
