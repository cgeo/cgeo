package cgeo.geocaching.connector.oc;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.Settings;
import cgeo.geocaching.enumerations.CacheType;

import org.apache.commons.lang3.StringUtils;

import android.text.Html;

import java.util.Collection;

public class OCXMLTest extends CGeoTestCase {

    public static void testOCGetCache() {
        final String geoCode = "OCDE76";

        final Geocache cache = OCXMLClient.getCache(geoCode, "");
        assertNotNull(cache);
        assertEquals(geoCode, cache.getGeocode());
        assertEquals("Gitarrenspielplatz", cache.getName());
        assertEquals(CacheType.TRADITIONAL, cache.getType());
        assertEquals(2.0, cache.getDifficulty(), 0.1);
        assertEquals(2.0, cache.getTerrain(), 0.1);
    }

    public static void testOCLogAttendedAsFound() {

        final String geoCode = "OCD541";
        final Geocache cache = OCXMLClient.getCache(geoCode, "ra_sch");
        assertNotNull(cache);

        assertTrue(cache.isFound());
    }

    public static void testOCOwner() {
        final String geoCode = "OCC9BE";
        final Geocache cache = OCXMLClient.getCache(geoCode, "andi12.2");
        assertNotNull(cache);

        assertTrue(cache.isOwner());
        assertEquals("180571", cache.getOwnerUserId());
    }

    public static void testOC0537Description() {
        final String geoCode = "OC0537";
        final Geocache cache = OCXMLClient.getCache(geoCode, "");
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
            final Geocache cache = OCXMLClient.getCache("OCD541", "");
            assertNotNull(cache);
            assertTrue(cache.isArchived());
            // Get nearby for this cache
            final Collection<Geocache> caches = OCXMLClient.getCachesAround(cache.getCoords(), 0.5, "");
            // Should not be in the result!
            assertFalse(caches.contains(cache));
        } finally {
            Settings.setExcludeDisabledCaches(oldExcludeDisabled);
            Settings.setExcludeMine(oldExcludeMine);
        }
    }

    public static void testFetchTwiceDuplicatesDescription() {
        final String geoCode = "OCEFBA";
        final String description = "Bei dem Cache kannst du einen kleinen Schatz bergen. Bitte lege aber einen ander Schatz in das Döschen. Achtung vor Automuggels.";

        deleteCacheFromDB(geoCode);
        Geocache cache = OCXMLClient.getCache(geoCode, "");
        assertNotNull(cache);
        try {
            assertEquals(geoCode, cache.getGeocode());
            // ignore copyright as the date part will change all the time
            assertEquals(description, removeCopyrightAndTags(cache.getDescription()));
            cache.store(null);

            // reload, make sure description is not duplicated
            cache = OCXMLClient.getCache(geoCode, "");
            assertNotNull(cache);
            assertEquals(description, removeCopyrightAndTags(cache.getDescription()));
        } finally {
            deleteCacheFromDB(geoCode);
        }
    }

    private static String removeCopyrightAndTags(String input) {
        return Html.fromHtml(StringUtils.substringBefore(input, "&copy")).toString().trim();
    }

    public static void testRemoveMarkup() {
        assertEquals("", OC11XMLParser.stripEmptyText(""));
        assertEquals("Test", OC11XMLParser.stripEmptyText("Test"));
        assertEquals("<b>bold and others not removed</b>", OC11XMLParser.stripEmptyText("<b>bold and others not removed</b>"));
        assertEquals("unnecessary paragraph", OC11XMLParser.stripEmptyText("<p>unnecessary paragraph</p>"));
        assertEquals("unnecessary span", OC11XMLParser.stripEmptyText("<span>unnecessary span</span>"));
        assertEquals("nested", OC11XMLParser.stripEmptyText("<span><span>nested</span></span>"));
        assertEquals("mixed", OC11XMLParser.stripEmptyText("<span> <p> mixed </p> </span>"));
        assertEquals("<p>not</p><p>removable</p>", OC11XMLParser.stripEmptyText("<p>not</p><p>removable</p>"));
    }
}
