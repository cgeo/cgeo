package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class CacheAttributeTest extends AndroidTestCase {

    public static void testTrimAttributeName() {
        for (CacheAttribute attribute : CacheAttribute.values()) {
            final String rawName = attribute.rawName;
            assertTrue("bad attribute name " + rawName, CacheAttribute.trimAttributeName(rawName).equals(rawName));
        }
    }

    public static void testIds() {
        for (CacheAttribute attribute : CacheAttribute.values()) {
            if (attribute != CacheAttribute.UNKNOWN) {
                assertTrue(attribute.rawName != null);
                assertTrue(attribute.rawName.length() > 0);
                assertTrue(attribute.drawableId != 0);
                assertTrue(attribute.stringIdYes != 0);
                assertTrue(attribute.stringIdNo != 0);
            }
        }
    }

    public static void testGetL10n() {
        final CacheAttribute attribute = CacheAttribute.HIKING;
        // This test is language dependend. It does not make sense to test it
        // with every attribute. We just want to know if getL10n works
        // correctly
        assertFalse("_yes and _no must not have the same translation",
                attribute.getL10n(true).equals(attribute.getL10n(false)));
    }

    public static void testGetBy() {
        final CacheAttribute attribute = CacheAttribute.HIKING; // an attribute that is present in GC and OC
        assertTrue("Test cannot be run with this attribute", attribute.gcid >= 0);
        assertTrue("Test cannot be run with this attribute", attribute.ocid >= 0);
        assertSame(CacheAttribute.getByRawName(attribute.rawName), attribute);
        assertSame(CacheAttribute.getByGcId(attribute.gcid), attribute);
        assertSame(CacheAttribute.getByOcId(attribute.ocid), attribute);
    }

    public static void testIsEnabled() {
        final CacheAttribute attribute = CacheAttribute.HIKING;
        final String hiking_yes = attribute.getAttributeName(true);
        final String hiking_no = attribute.getAttributeName(false);
        assertTrue(CacheAttribute.isEnabled(hiking_yes));
        assertFalse(CacheAttribute.isEnabled(hiking_no));
    }

}
