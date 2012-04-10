package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class CacheAttributeTest extends AndroidTestCase {

    public static void testTrimAttributeName() {
        for (CacheAttribute attribute : CacheAttribute.values()) {
            final String rawName = attribute.gcRawName;
            assertTrue("bad attribute name " + rawName, CacheAttribute.trimAttributeName(rawName).equals(rawName));
        }
    }
}
