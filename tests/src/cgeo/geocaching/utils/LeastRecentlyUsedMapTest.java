package cgeo.geocaching.utils;

import cgeo.geocaching.utils.LeastRecentlyUsedMap.OperationModes;

import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

public class LeastRecentlyUsedMapTest extends TestCase {

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

    public static void testLruMode() {
        Map<String, String> map = new LeastRecentlyUsedMap<String, String>(4, OperationModes.LRU_CACHE);
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        // keep in cache
        map.get("one");
        map.put("four", "4");
        map.put("five", "5");
        map.put("six", "6");
        // keep in cache
        map.get("one");
        // re-add
        map.put("five", "5");
        map.put("seven", "7");

        assertEquals("six, one, five, seven", colToStr(map.keySet()));
    }
    
    public static void testBoundedMode() {
        Map<String, String> map = new LeastRecentlyUsedMap<String, String>(5, OperationModes.BOUNDED);
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        // read does not change anything
        map.get("one");
        map.put("four", "4");
        // re-put should update the order
        map.put("three", "3");
        map.put("five", "5");
        // read does not change anything
        map.get("one");
        map.put("six", "6");
        map.put("seven", "7");

        assertEquals("four, three, five, six, seven", colToStr(map.keySet()));
    }

    public static void testBoundedIgnoreReinsertMode() {
        Map<String, String> map = new LeastRecentlyUsedMap<String, String>(5, OperationModes.BOUNDED_IGNORE_REINSERT);
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        // read does not change anything
        map.get("one");
        map.put("four", "4");
        // re-put should update the order
        map.put("two", "2");
        map.put("five", "5");
        // read does not change anything
        map.get("one");
        map.put("six", "6");
        map.put("seven", "7");

        assertEquals("three, four, five, six, seven", colToStr(map.keySet()));
    }

}
