package cgeo.geocaching.network;

import junit.framework.TestCase;

public class NetworkTest extends TestCase {

    public static void testRfc3986URLEncode() {
        assertEquals("*", Network.rfc3986URLEncode("*"));
        assertEquals("~", Network.rfc3986URLEncode("~"));
        assertEquals("%20", Network.rfc3986URLEncode(" "));
    }

}
