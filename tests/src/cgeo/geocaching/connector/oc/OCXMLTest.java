package cgeo.geocaching.connector.oc;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.Settings;
import cgeo.geocaching.enumerations.CacheType;

import java.util.Collection;

public class OCXMLTest extends CGeoTestCase {

    public static void testOCGetCache() {
        final String geoCode = "OCDE76";

        final Geocache cache = OCXMLClient.getCache(geoCode);
        assertNotNull(cache);
        assertEquals(geoCode, cache.getGeocode());
        assertEquals("Gitarrenspielplatz", cache.getName());
        assertEquals(CacheType.TRADITIONAL, cache.getType());
        assertEquals(2.0, cache.getDifficulty(), 0.1);
        assertEquals(2.0, cache.getTerrain(), 0.1);
    }

    public static void testOCLogAttendedAsFound() {

        final String oldOCName = Settings.getOCConnectorUserName();
        try {
            Settings.setOCConnectorUserName("ra_sch");
            final String geoCode = "OCD541";
            final Geocache cache = OCXMLClient.getCache(geoCode);
            assertNotNull(cache);

            assertTrue(cache.isFound());
        } finally {
            Settings.setOCConnectorUserName(oldOCName);
        }
    }

    public static void testOCOwner() {
        final String oldOCName = Settings.getOCConnectorUserName();
        try {
            Settings.setOCConnectorUserName("andi12.2");
            final String geoCode = "OCC9BE";
            final Geocache cache = OCXMLClient.getCache(geoCode);
            assertNotNull(cache);

            assertTrue(cache.isOwner());
            assertEquals("180571", cache.getOwnerUserId());
        } finally {
            Settings.setOCConnectorUserName(oldOCName);
        }
    }

    public static void testOC0537Description() {
        final String geoCode = "OC0537";
        final Geocache cache = OCXMLClient.getCache(geoCode);
        assertNotNull(cache);

        assertFalse(cache.getDescription().length() < 100);
    }

    public static void testNoArchivedInNearby() {

        final boolean oldExcludeDisabled = Settings.isExcludeDisabledCaches();
        final boolean oldExcludeMine = Settings.isExcludeMyCaches();
        try {
            Settings.setExcludeDisabledCaches(false);
            Settings.setExcludeMine(false);
            // get an archived cache
            final Geocache cache = OCXMLClient.getCache("OCD541");
            assertNotNull(cache);
            assertTrue(cache.isArchived());
            // Get nearby for this cache
            final Collection<Geocache> caches = OCXMLClient.getCachesAround(cache.getCoords(), 0.5);
            // Should not be in the result!
            assertFalse(caches.contains(cache));
        } finally {
            Settings.setExcludeDisabledCaches(oldExcludeDisabled);
            Settings.setExcludeMine(oldExcludeMine);
        }
    }

    public static void testFetchTwiceDuplicatesDescription() {
        final String geoCode = "OCEFBA";
        final String description = "<p><span>Bei dem Cache kannst du einen kleinen Schatz bergen. Bitte lege aber einen ander Schatz in das Döschen. Achtung vor Automuggels.</span></p>";

        deleteCacheFromDB(geoCode);
        Geocache cache = OCXMLClient.getCache(geoCode);
        assertNotNull(cache);
        try {
            assertEquals(geoCode, cache.getGeocode());
            assertEquals(description, cache.getDescription());
            cache.store(null);

            // reload, make sure description is not duplicated
            cache = OCXMLClient.getCache(geoCode);
            assertNotNull(cache);
            assertEquals(description, cache.getDescription());
        } finally {
            deleteCacheFromDB(geoCode);
        }
    }
}
