package cgeo.geocaching.connector.oc;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.cgData;
import cgeo.geocaching.enumerations.LoadFlags;

public class OkapiClientTest extends CGeoTestCase {

    public static void testGetOCCache() {
        final String geoCode = "OU0331";
        Geocache cache = OkapiClient.getCache(geoCode);
        assertNotNull(cache);
        assertEquals(geoCode, cache.getGeocode());
        assertEquals("Oshkosh Municipal Tank", cache.getName());
        assertTrue(cache.isDetailed());
        // cache should be stored to DB (to listID 0) when loaded above
        cache = cgData.loadCache(geoCode, LoadFlags.LOAD_ALL_DB_ONLY);
        assertNotNull(cache);
        assertEquals(geoCode, cache.getGeocode());
        assertEquals("Oshkosh Municipal Tank", cache.getName());
        assertTrue(cache.isDetailed());
    }

    public static void testOCSearchMustWorkWithoutOAuthAccessTokens() {
        final String geoCode = "OC1234";
        Geocache cache = OkapiClient.getCache(geoCode);
        assertNotNull("You must have a valid OKAPI key installed for running this test (but you do not need to set credentials in the app).", cache);
        assertEquals("Wupper-Schein", cache.getName());
    }
}
