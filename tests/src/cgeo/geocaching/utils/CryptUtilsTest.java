package cgeo.geocaching.utils;

import junit.framework.TestCase;

public class CryptUtilsTest extends TestCase {
    public static void testROT13() {
        assertEquals("", CryptUtils.rot13(""));
        assertEquals("", CryptUtils.rot13((String) null));
        assertEquals("Pnpur uvag", CryptUtils.rot13("Cache hint"));
        assertEquals("123", CryptUtils.rot13("123"));
    }

    public static void testConvertToGcBase31() {
        assertEquals("1186660", CryptUtils.convertToGcBase31("GC1PKK9"));
        assertEquals("4660", CryptUtils.convertToGcBase31("GC1234"));
        assertEquals("61731", CryptUtils.convertToGcBase31("GCF123"));
    }
}
