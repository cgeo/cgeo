package cgeo.geocaching.connector.opencaching;

import cgeo.CGeoTestCase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgData;
import cgeo.geocaching.connector.oc.OkapiClient;
import cgeo.geocaching.enumerations.LoadFlags;

public class OkapiClientTest extends CGeoTestCase {

    public static void testGetOCCache() {
        String geoCode = "OU0331";
        cgCache cache = OkapiClient.getCache(geoCode);
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

}
