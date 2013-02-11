package cgeo.geocaching.connector.opencaching;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.Settings;
import cgeo.geocaching.connector.oc.OCXMLClient;
import cgeo.geocaching.enumerations.CacheType;

public class OCXMLTest extends CGeoTestCase {

    public static void testOCGetCache() {
        String geoCode = "OCDE76";

        Geocache cache = OCXMLClient.getCache(geoCode);
        assertNotNull(cache);
        assertEquals(geoCode, cache.getGeocode());
        assertEquals("Gitarrenspielplatz", cache.getName());
        assertEquals(CacheType.TRADITIONAL, cache.getType());
        assertEquals(2.0, cache.getDifficulty(), 0.1);
        assertEquals(2.0, cache.getTerrain(), 0.1);
    }

    public static void testOCLogAttendedAsFound() {

        String oldOCName = Settings.getOCConnectorUserName();
        try {
            Settings.setOCConnectorUserName("ra_sch");
            String geoCode = "OCD541";
            Geocache cache = OCXMLClient.getCache(geoCode);
            assertNotNull(cache);

            assertTrue(cache.isFound());
        } finally {
            Settings.setOCConnectorUserName(oldOCName);
        }
    }

    public static void testOCOwner() {
        String oldOCName = Settings.getOCConnectorUserName();
        try {
            Settings.setOCConnectorUserName("andi12.2");
            String geoCode = "OCC9BE";
            Geocache cache = OCXMLClient.getCache(geoCode);
            assertNotNull(cache);

            assertTrue(cache.isOwner());
        } finally {
            Settings.setOCConnectorUserName(oldOCName);
        }
    }
}
