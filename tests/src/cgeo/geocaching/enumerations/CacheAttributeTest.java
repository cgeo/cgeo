package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class CacheAttributeTest extends AndroidTestCase {

    public static void testTrimAttributeName() {
        for (CacheAttribute attribute : CacheAttribute.values()) {
            final String rawName = attribute.gcRawName;
            assertTrue("bad attribute name " + rawName, CacheAttribute.trimAttributeName(rawName).equals(rawName));
        }
    }

    public static void testIds() {
        for (CacheAttribute attribute : CacheAttribute.values()) {
            if (attribute != CacheAttribute.UNKNOWN) {
                assertTrue(attribute.drawableId != 0);
                assertTrue(attribute.stringIdYes != 0);
                assertTrue(attribute.stringIdNo != 0);
            }
        }
    }
}
