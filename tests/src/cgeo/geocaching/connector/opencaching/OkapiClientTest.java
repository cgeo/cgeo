package cgeo.geocaching.connector.opencaching;

import cgeo.geocaching.cgCache;

import android.test.AndroidTestCase;

public class OkapiClientTest extends AndroidTestCase {

    public static void testGetOCCache() {
        String geoCode = "OU0331";
        cgCache cache = OkapiClient.getCache(geoCode);
        assertNotNull(cache);
        assertEquals(geoCode, cache.getGeocode());
        assertEquals("Oshkosh Municipal Tank", cache.getName());
    }

}
