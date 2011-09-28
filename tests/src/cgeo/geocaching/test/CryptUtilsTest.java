package cgeo.geocaching.test;

import cgeo.geocaching.utils.CryptUtils;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class CryptUtilsTest extends TestCase {
    public void testROT13() {
        assertEquals("", CryptUtils.rot13(""));
        assertEquals("", CryptUtils.rot13((String) null));
        assertEquals("Pnpur uvag", CryptUtils.rot13("Cache hint"));
        assertEquals("123", CryptUtils.rot13("123"));
    }
}
