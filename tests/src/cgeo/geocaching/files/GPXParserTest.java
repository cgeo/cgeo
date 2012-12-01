package cgeo.geocaching.files;

import cgeo.geocaching.LogEntry;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GPXParserTest extends AbstractResourceInstrumentationTestCase {
    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); // 2010-04-20T07:00:00Z
    private int listId;

    public void testGPXVersion100() throws Exception {
        testGPXVersion(R.raw.gc1bkp3_gpx100);
    }

    private cgCache testGPXVersion(final int resourceId) throws IOException, ParserException {
        final List<cgCache> caches = readGPX10(resourceId);
        assertNotNull(caches);
        assertEquals(1, caches.size());
        final cgCache cache = caches.get(0);
        assertEquals("GC1BKP3", cache.getGeocode());
        assertEquals("9946f030-a514-46d8-a050-a60e92fd2e1a", cache.getGuid());
        assertEquals(CacheType.TRADITIONAL, cache.getType());
        assertEquals(false, cache.isArchived());
        assertEquals(false, cache.isDisabled());
        assertEquals("Die Schatzinsel / treasure island", cache.getName());
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.getOwnerDisplayName());
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.getOwnerUserId());
        assertEquals(CacheSize.MICRO, cache.getSize());
        assertEquals(1.0f, cache.getDifficulty());
        assertEquals(5.0f, cache.getTerrain());
        assertEquals("Baden-Württemberg, Germany", cache.getLocation());
        assertEquals("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel.\nA old dream of my childhood, a treasure on a lonely island.", cache.getShortdesc());
        assertEquals(new Geopoint(48.859683, 9.1874), cache.getCoords());
        return cache;
    }

    public void testGPXVersion101() throws IOException, ParserException {
        final cgCache cache = testGPXVersion(R.raw.gc1bkp3_gpx101);
        assertNotNull(cache.getAttributes());
        assertEquals(10, cache.getAttributes().size());
    }

    public void testOC() throws IOException, ParserException {
        final List<cgCache> caches = readGPX10(R.raw.oc5952_gpx);
        final cgCache cache = caches.get(0);
        assertEquals("OC5952", cache.getGeocode());
        assertEquals(CacheType.TRADITIONAL, cache.getType());
        assertEquals(false, cache.isArchived());
        assertEquals(false, cache.isDisabled());
        assertEquals("Die Schatzinsel / treasure island", cache.getName());
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.getOwnerDisplayName());
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.getOwnerUserId());
        assertEquals(CacheSize.SMALL, cache.getSize());
        assertEquals(1.0f, cache.getDifficulty());
        assertEquals(4.0f, cache.getTerrain());
        assertEquals("Baden-Württemberg, Germany", cache.getLocation());
        assertEquals("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel. A old dream of my childhood, a treasure on a lonely is", cache.getShortdesc());
        assertEquals(new Geopoint(48.85968, 9.18740), cache.getCoords());
        assertTrue(cache.isReliableLatLon());
    }

    public void testGc31j2h() throws IOException, ParserException {
        removeCacheCompletely("GC31J2H");
        final List<cgCache> caches = readGPX10(R.raw.gc31j2h);
        assertEquals(1, caches.size());
        final cgCache cache = caches.get(0);

        assertGc31j2h(cache);
        assertSame(cache, caches.get(0));

        // no waypoints without importing waypoint file
        assertEquals(0, cache.getWaypoints().size());
        assertTrue(cache.isReliableLatLon());
    }

    public void testGc31j2hWpts() throws IOException, ParserException {
        removeCacheCompletely("GC31J2H");
        List<cgCache> caches = readGPX10(R.raw.gc31j2h, R.raw.gc31j2h_wpts);
        assertEquals(1, caches.size());
        cgCache cache = caches.get(0);
        assertGc31j2h(cache);
        assertGc31j2hWaypoints(cache);
    }

    public void testGc31j2hWptsWithoutCache() throws IOException, ParserException {
        final List<cgCache> caches = readGPX10(R.raw.gc31j2h_wpts);
        assertEquals(0, caches.size());
    }

    public static void testConvertWaypointSym2Type() {
        assertEquals(WaypointType.WAYPOINT, GPXParser.convertWaypointSym2Type("unknown sym"));

        assertEquals(WaypointType.PARKING, GPXParser.convertWaypointSym2Type("Parking area"));
        assertEquals(WaypointType.STAGE, GPXParser.convertWaypointSym2Type("Stages of a multicache"));
        assertEquals(WaypointType.PUZZLE, GPXParser.convertWaypointSym2Type("Question to answer"));
        assertEquals(WaypointType.TRAILHEAD, GPXParser.convertWaypointSym2Type("Trailhead"));
        assertEquals(WaypointType.FINAL, GPXParser.convertWaypointSym2Type("Final location"));
        assertEquals(WaypointType.WAYPOINT, GPXParser.convertWaypointSym2Type("Reference point"));

        assertEquals(WaypointType.PARKING, GPXParser.convertWaypointSym2Type(WaypointType.PARKING.getL10n()));
    }

    private static void assertGc31j2h(final cgCache cache) {
        assertEquals("GC31J2H", cache.getGeocode());
        assertEquals("Hockenheimer City-Brunnen", cache.getName());
        assertTrue("Hockenheimer City-Brunnen by vptsz, Multi-cache (2/1)", cache.getShortDescription().startsWith("Kurzer informativer Multi entlang der Brunnen"));
        assertTrue(cache.getDescription().startsWith("Cachemobile können kostenfrei am Messplatz geparkt werden."));
        assertTrue(cache.hasTrackables());
        assertEquals(2.0f, cache.getDifficulty(), 0.01f);
        assertEquals(1.0f, cache.getTerrain(), 0.01f);
        final Geopoint refCoordinates = new Geopoint("N 49° 19.122", "E 008° 32.739");
        assertEquals(refCoordinates, cache.getCoords());
        assertEquals("vptsz", cache.getOwnerDisplayName());
        assertEquals("vptsz", cache.getOwnerUserId());
        assertEquals(CacheSize.SMALL, cache.getSize());
        assertEquals(CacheType.MULTI, cache.getType());
        assertFalse(cache.isArchived());
        assertFalse(cache.isDisabled());
        assertFalse(cache.isEventCache());
        assertFalse(cache.isPremiumMembersOnly());
        assertFalse(cache.isOwn());
        assertTrue(cache.isFound());
        assertEquals("Station3: Der zerbrochene Stein zählt doppelt.\nFinal: Oben neben dem Tor", cache.getHint());
        // logs
        assertEquals(6, cache.getLogs().size());
        final LogEntry log = cache.getLogs().get(5);
        assertEquals("Geoteufel", log.author);
        assertEquals(parseTime("2011-09-11T07:00:00Z"), log.date);
        assertEquals(-1, log.found);
        assertEquals("Sehr schöne Runde und wir haben wieder etwas Neues über Hockenheim gelernt. Super Tarnung.\nTFTC, Geoteufel", log.log);

        // following info is not contained in pocket query gpx file
        assertEquals(0, cache.getAttributes().size());
    }

    private static long parseTime(final String time) {
        try {
            return LOG_DATE_FORMAT.parse(time).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    private static void assertGc31j2hWaypoints(final cgCache cache) {
        assertNotNull(cache.getWaypoints());
        assertEquals(2, cache.getWaypoints().size());
        cgWaypoint wp = cache.getWaypoints().get(1);
        assertEquals("GC31J2H", wp.getGeocode());
        assertEquals("00", wp.getPrefix());
        assertEquals("---", wp.getLookup());
        assertEquals("Parkplatz", wp.getName());
        assertEquals("Kostenfreies Parken (je nach Parkreihe Parkscheibe erforderlich)", wp.getNote());
        assertEquals(WaypointType.PARKING, wp.getWaypointType());
        assertEquals(49.317517, wp.getCoords().getLatitude(), 0.000001);
        assertEquals(8.545083, wp.getCoords().getLongitude(), 0.000001);

        wp = cache.getWaypoints().get(0);
        assertEquals("GC31J2H", wp.getGeocode());
        assertEquals("S1", wp.getPrefix());
        assertEquals("---", wp.getLookup());
        assertEquals("Station 1", wp.getName());
        assertEquals("Ein zweiter Wegpunkt, der nicht wirklich existiert sondern nur zum Testen gedacht ist.", wp.getNote());
        assertEquals(WaypointType.STAGE, wp.getWaypointType());
        assertEquals(49.317500, wp.getCoords().getLatitude(), 0.000001);
        assertEquals(8.545100, wp.getCoords().getLongitude(), 0.000001);
    }

    private List<cgCache> readGPX10(int... resourceIds) throws IOException, ParserException {
        final GPX10Parser parser = new GPX10Parser(listId);
        return readVersionedGPX(parser, resourceIds);
    }

    private List<cgCache> readGPX11(int... resourceIds) throws IOException, ParserException {
        final GPX11Parser parser = new GPX11Parser(listId);
        return readVersionedGPX(parser, resourceIds);
    }

    private List<cgCache> readVersionedGPX(final GPXParser parser, int... resourceIds) throws IOException, ParserException {
        final Set<String> result = new HashSet<String>();
        for (int resourceId : resourceIds) {
            final InputStream instream = getResourceStream(resourceId);
            try {
                Collection<cgCache> caches = parser.parse(instream, null);
                assertNotNull(caches);
                for (cgCache cache : caches) {
                    result.add(cache.getGeocode());
                }
            } finally {
                instream.close();
            }
        }
        // reload caches, because the parser only returns the minimum version of each cache
        return new ArrayList<cgCache>(cgData.loadCaches(result, LoadFlags.LOAD_ALL_DB_ONLY));
    }

    public static void testParseDateWithFractionalSeconds() {
        // was experienced in GSAK file
        final String dateString = "2011-08-13T02:52:18.103Z";
        try {
            GPXParser.parseDate(dateString);
        } catch (ParseException e) {
            fail();
            e.printStackTrace();
        }
    }

    public static void testParseDateWithHugeFraction() {
        // see issue 821
        String dateString = "2011-11-07T00:00:00.0000000-07:00";
        try {
            GPXParser.parseDate(dateString);
        } catch (ParseException e) {
            fail();
            e.printStackTrace();
        }
    }

    public void testSelfmadeGPXWithoutGeocodes() throws Exception {
        final List<cgCache> caches = readGPX11(R.raw.no_connector);
        assertEquals(13, caches.size());
    }

    public void testTexasChallenge2012() throws Exception {
        final List<cgCache> caches = readGPX10(R.raw.challenge);
        // previously these caches overwrote each other during parsing
        assertEquals(130, caches.size());
    }

    public void testGeoToad() throws Exception {
        final List<cgCache> caches = readGPX10(R.raw.geotoad);
        assertEquals(2, caches.size());
        List<String> codes = new ArrayList<String>();
        for (cgCache cache : caches) {
            codes.add(cache.getGeocode());
        }
        assertTrue(codes.contains("GC2KN6K"));
        assertTrue(codes.contains("GC1T3MK"));
    }

    public void testLazyLogLoading() throws IOException, ParserException {
        // this test should be in CacheTest, but it is easier to create here due to the GPX import abilities
        final String geocode = "GC31J2H";
        removeCacheCompletely(geocode);
        final List<cgCache> caches = readGPX10(R.raw.lazy);
        assertEquals(1, caches.size());
        cgData.removeAllFromCache();
        // load only the minimum cache, it has several members missing
        final cgCache minimalCache = cgData.loadCache(geocode, EnumSet.of(LoadFlag.LOAD_DB_MINIMAL));

        // now check that we load lazy members on demand
        assertFalse(minimalCache.getAttributes().isEmpty());
        assertFalse(minimalCache.getLogs().isEmpty());

        removeCacheCompletely(geocode);
    }

    public void testDuplicateImport() throws IOException, ParserException {
        final String geocode = "GC31J2H";
        removeCacheCompletely(geocode);

        // first import
        List<cgCache> caches = readGPX10(R.raw.lazy);
        assertEquals(1, caches.size());
        assertEquals(6, caches.get(0).getLogs().size());

        // second import
        caches = readGPX10(R.raw.lazy);
        assertEquals(1, caches.size());
        assertEquals(6, caches.get(0).getLogs().size());

        removeCacheCompletely(geocode);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        listId = cgData.createList("Temporary unit testing");
        assertTrue(listId != StoredList.TEMPORARY_LIST_ID);
        assertTrue(listId != StoredList.STANDARD_LIST_ID);
    }

    @Override
    protected void tearDown() throws Exception {
        SearchResult search = cgData.getBatchOfStoredCaches(null, CacheType.ALL, listId);
        assertNotNull(search);
        cgData.removeCaches(search.getGeocodes(), LoadFlags.REMOVE_ALL);
        cgData.removeList(listId);
        super.tearDown();
    }
}
