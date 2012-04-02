package cgeo.geocaching.utils;

import cgeo.geocaching.utils.LeastRecentlyUsedMap.OperationModes;

import java.util.Collection;
import java.util.Set;

import junit.framework.TestCase;

public class LeastRecentlyUsedSetTest extends TestCase {

    private static String colToStr(Collection<?> col)
    {
        StringBuilder strb = new StringBuilder();
        boolean first = true;
        for (Object o : col) {
            if (!first) {
                strb.append(", ");
            }
            first = false;
            strb.append(o.toString());
        }
        return strb.toString();
    }

    public static void testBoundedMode() {
        Set<String> set = new LeastRecentlyUsedSet<String>(5, OperationModes.BOUNDED);
        set.add("one");
        set.add("two");
        set.add("three");
        // read does not change anything
        set.contains("one");
        set.add("four");
        // re-put should update the order
        set.add("three");
        set.add("five");
        // read does not change anything
        set.contains("one");
        set.add("six");
        set.add("seven");

        assertEquals("four, three, five, six, seven", colToStr(set));
    }

    public static void testLruMode() {
        // the same as behaviour as BOUNDED

        Set<String> set = new LeastRecentlyUsedSet<String>(5, OperationModes.LRU_CACHE);
        set.add("one");
        set.add("two");
        set.add("three");
        // read does not change anything
        set.contains("one");
        set.add("four");
        // re-put should update the order
        set.add("three");
        set.add("five");
        // read does not change anything
        set.contains("one");
        set.add("six");
        set.add("seven");

        assertEquals("four, three, five, six, seven", colToStr(set));
    }

    public static void testBoundedIgnoreReinsertMode() {
        Set<String> set = new LeastRecentlyUsedSet<String>(5, OperationModes.BOUNDED_IGNORE_REINSERT);
        set.add("one");
        set.add("two");
        set.add("three");
        // read does not change anything
        set.contains("one");
        set.add("four");
        // re-put should update the order
        set.add("two");
        set.add("five");
        // read does not change anything
        set.contains("one");
        set.add("six");
        set.add("seven");

        assertEquals("three, four, five, six, seven", colToStr(set));
    }

}
