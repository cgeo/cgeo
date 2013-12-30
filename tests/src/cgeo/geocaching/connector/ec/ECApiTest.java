package cgeo.geocaching.connector.ec;

import junit.framework.TestCase;

public class ECApiTest extends TestCase {

    public static void testGetIdFromGeocode() throws Exception {
        assertEquals("242", ECApi.getIdFromGeocode("EC242"));
        assertEquals("242", ECApi.getIdFromGeocode("ec242"));
    }

}
