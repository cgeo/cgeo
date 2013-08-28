package cgeo.geocaching.utils;

import cgeo.geocaching.connector.gc.GCConstants;

import junit.framework.TestCase;

public class CryptUtilsTest extends TestCase {
    public static void testROT13() {
        assertEquals("", CryptUtils.rot13(""));
        assertEquals("", CryptUtils.rot13((String) null));
        assertEquals("Pnpur uvag", CryptUtils.rot13("Cache hint"));
        assertEquals("Pnpur [plain] uvag", CryptUtils.rot13("Cache [plain] hint"));
        assertEquals("[all plain]", CryptUtils.rot13("[all plain]"));
        assertEquals("123", CryptUtils.rot13("123"));
    }

    public static void testConvertToGcBase31() {
        assertEquals(1186660, GCConstants.gccodeToGCId("GC1PKK9"));
        assertEquals(4660, GCConstants.gccodeToGCId("GC1234"));
        assertEquals(61731, GCConstants.gccodeToGCId("GCF123"));
    }

    public static void testIssue1902() {
        assertEquals("ƖƖlƖƖƖƖ", CryptUtils.rot13("ƖƖyƖƖƖƖ"));
    }

    public static void testSha1() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", CryptUtils.sha1(""));
        // expected value taken from debugger. should assure every developer uses UTF-8
        assertEquals("cf2f343f59cea81afc0a5a566cb138ba349c548f", CryptUtils.sha1("äöü"));
    }

    public static void testMd5() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", CryptUtils.md5(""));
        // expected value taken from debugger. should assure every developer uses UTF-8
        assertEquals("a7f4e3ec08f09be2ef7ecb4eea5f8981", CryptUtils.md5("äöü"));
    }
}
