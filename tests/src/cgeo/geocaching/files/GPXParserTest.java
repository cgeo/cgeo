package cgeo.geocaching.files;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.DateUtils;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GPXParserTest extends AbstractResourceInstrumentationTestCase {
    private static final SynchronizedDateFormat LOG_DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); // 2010-04-20T07:00:00Z

    public void testGPXVersion100() throws Exception {
        testGPXVersion(R.raw.gc1bkp3_gpx100);
    }

    private Geocache testGPXVersion(final int resourceId) throws IOException, ParserException {
        final List<Geocache> caches = readGPX10(resourceId);
        assertThat(caches).isNotNull();
        assertThat(caches).hasSize(1);
        final Geocache cache = caches.get(0);
        assertThat(cache.getGeocode()).isEqualTo("GC1BKP3");
        assertThat(cache.getGuid()).isEqualTo("9946f030-a514-46d8-a050-a60e92fd2e1a");
        assertThat(cache.getType()).isEqualTo(CacheType.TRADITIONAL);
        assertThat(cache.isArchived()).isEqualTo(false);
        assertThat(cache.isDisabled()).isEqualTo(false);
        assertThat(cache.getName()).isEqualTo("Die Schatzinsel / treasure island");
        assertThat(cache.getOwnerDisplayName()).isEqualTo("Die unbesiegbaren Geo - Geparden");
        assertThat(cache.getOwnerUserId()).isEqualTo("Die unbesiegbaren Geo - Geparden");
        assertThat(cache.getSize()).isEqualTo(CacheSize.MICRO);
        assertThat(cache.getDifficulty()).isEqualTo(1.0f);
        assertThat(cache.getTerrain()).isEqualTo(5.0f);
        assertThat(cache.getLocation()).isEqualTo("Baden-Württemberg, Germany");
        assertThat(cache.getShortDescription()).isEqualTo("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel.\nA old dream of my childhood, a treasure on a lonely island.");
        assertThat(cache.getCoords()).isEqualTo(new Geopoint(48.859683, 9.1874));
        return cache;
    }

    public void testGPXVersion101() throws IOException, ParserException {
        final Geocache cache = testGPXVersion(R.raw.gc1bkp3_gpx101);
        assertThat(cache.getAttributes()).isNotNull();
        assertThat(cache.getAttributes()).hasSize(10);
    }

    public void testOC() throws IOException, ParserException {
        final List<Geocache> caches = readGPX10(R.raw.oc5952_gpx);
        final Geocache cache = caches.get(0);
        assertThat(cache.getGeocode()).isEqualTo("OC5952");
        assertThat(cache.getType()).isEqualTo(CacheType.TRADITIONAL);
        assertThat(cache.isArchived()).isEqualTo(false);
        assertThat(cache.isDisabled()).isEqualTo(false);
        assertThat(cache.getName()).isEqualTo("Die Schatzinsel / treasure island");
        assertThat(cache.getOwnerDisplayName()).isEqualTo("Die unbesiegbaren Geo - Geparden");
        assertThat(cache.getOwnerUserId()).isEqualTo("Die unbesiegbaren Geo - Geparden");
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL);
        assertThat(cache.getDifficulty()).isEqualTo(1.0f);
        assertThat(cache.getTerrain()).isEqualTo(4.0f);
        assertThat(cache.getLocation()).isEqualTo("Baden-Württemberg, Germany");
        assertThat(cache.getShortDescription()).isEqualTo("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel. A old dream of my childhood, a treasure on a lonely is");
        assertThat(cache.getCoords()).isEqualTo(new Geopoint(48.85968, 9.18740));
        assertThat(cache.isReliableLatLon()).isTrue();
    }

    public void testGc31j2h() throws IOException, ParserException {
        removeCacheCompletely("GC31J2H");
        final List<Geocache> caches = readGPX10(R.raw.gc31j2h);
        assertThat(caches).hasSize(1);
        final Geocache cache = caches.get(0);

        assertGc31j2h(cache);
        assertThat(caches.get(0)).isSameAs(cache);

        // no waypoints without importing waypoint file
        assertThat(cache.getWaypoints()).isEmpty();
        assertThat(cache.isReliableLatLon()).isTrue();
    }

    public void testGc31j2hWpts() throws IOException, ParserException {
        removeCacheCompletely("GC31J2H");
        final List<Geocache> caches = readGPX10(R.raw.gc31j2h, R.raw.gc31j2h_wpts);
        assertThat(caches).hasSize(1);
        final Geocache cache = caches.get(0);
        assertGc31j2h(cache);
        assertGc31j2hWaypoints(cache);
    }

    public void testRenamedWaypointTypes() throws IOException, ParserException {
        removeCacheCompletely("GC31J2H");
        final List<Geocache> caches = readGPX10(R.raw.renamed_waypoints, R.raw.renamed_waypoints_wpts);
        assertThat(caches).hasSize(25);
        // multi waypoint (now "physical stage")
        Geocache cache = caches.get(12);
        assertThat(cache.getGeocode()).isEqualTo("GC3NBDE");
        List<Waypoint> waypoints = cache.getWaypoints();
        assertThat(waypoints).isNotEmpty();
        Waypoint waypoint = waypoints.get(1);
        assertThat(waypoint).isNotNull();
        assertThat(waypoint.getWaypointType()).isEqualTo(WaypointType.STAGE);
        // mystery waypoint - now "virtual stage"
        cache = caches.get(15);
        assertThat(cache.getGeocode()).isEqualTo("GC16CBG");
        waypoints = cache.getWaypoints();
        assertThat(waypoints).isNotEmpty();
        waypoint = waypoints.get(1);
        assertThat(waypoint).isNotNull();
        assertThat(waypoint.getWaypointType()).isEqualTo(WaypointType.PUZZLE);
    }

    public void testGc31j2hWptsWithoutCache() throws IOException, ParserException {
        final List<Geocache> caches = readGPX10(R.raw.gc31j2h_wpts);
        assertThat(caches).isEmpty();
    }

    public static void testConvertWaypointSym2Type() {
        assertThat(GPXParser.convertWaypointSym2Type("unknown sym")).isEqualTo(WaypointType.WAYPOINT);

        assertThat(GPXParser.convertWaypointSym2Type("Parking area")).isEqualTo(WaypointType.PARKING);
        assertThat(GPXParser.convertWaypointSym2Type("Stages of a multicache")).isEqualTo(WaypointType.STAGE);
        assertThat(GPXParser.convertWaypointSym2Type("Question to answer")).isEqualTo(WaypointType.PUZZLE);
        assertThat(GPXParser.convertWaypointSym2Type("Trailhead")).isEqualTo(WaypointType.TRAILHEAD);
        assertThat(GPXParser.convertWaypointSym2Type("Final location")).isEqualTo(WaypointType.FINAL);
        assertThat(GPXParser.convertWaypointSym2Type("Reference point")).isEqualTo(WaypointType.WAYPOINT);

        assertThat(GPXParser.convertWaypointSym2Type(WaypointType.PARKING.getL10n())).isEqualTo(WaypointType.PARKING);
        // new names of multi and mystery stages
        assertThat(GPXParser.convertWaypointSym2Type("Physical Stage")).isEqualTo(WaypointType.STAGE);
        assertThat(GPXParser.convertWaypointSym2Type("Virtual Stage")).isEqualTo(WaypointType.PUZZLE);
    }

    private static void assertGc31j2h(final Geocache cache) {
        assertThat(cache.getGeocode()).isEqualTo("GC31J2H");
        assertThat(cache.getName()).isEqualTo("Hockenheimer City-Brunnen");
        assertThat(cache.getShortDescription()).startsWith("Kurzer informativer Multi entlang der Brunnen");
        assertThat(cache.getDescription()).startsWith("Cachemobile können kostenfrei am Messplatz geparkt werden.");
        assertThat(cache.hasTrackables()).isTrue();
        assertEquals(2.0f, cache.getDifficulty(), 0.01f);
        assertEquals(1.0f, cache.getTerrain(), 0.01f);
        final Geopoint refCoordinates = new Geopoint("N 49° 19.122", "E 008° 32.739");
        assertThat(cache.getCoords()).isEqualTo(refCoordinates);
        assertThat(cache.getOwnerDisplayName()).isEqualTo("vptsz");
        assertThat(cache.getOwnerUserId()).isEqualTo("vptsz");
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL);
        assertThat(cache.getType()).isEqualTo(CacheType.MULTI);
        assertThat(cache.isArchived()).isFalse();
        assertThat(cache.isDisabled()).isFalse();
        assertThat(cache.isEventCache()).isFalse();
        assertThat(cache.isPremiumMembersOnly()).isFalse();
        assertThat(cache.isOwner()).isFalse();
        assertThat(cache.isFound()).isTrue();
        assertThat(cache.getHint()).isEqualTo("Station3: Der zerbrochene Stein zählt doppelt.\nFinal: Oben neben dem Tor");
        // logs
        assertThat(cache.getLogs()).hasSize(6);
        final LogEntry log = cache.getLogs().get(5);
        assertThat(log.author).isEqualTo("Geoteufel");
        assertThat(log.date).isEqualTo(parseTime("2011-09-11T07:00:00Z"));
        assertThat(log.found).isEqualTo(-1);
        assertThat(log.log).isEqualTo("Sehr schöne Runde und wir haben wieder etwas Neues über Hockenheim gelernt. Super Tarnung.\nTFTC, Geoteufel");
        assertThat(log.isOwn()).isFalse();
        assertThat(log.getDisplayText()).isEqualTo(log.log);
        assertThat(DateUtils.daysSince(log.date) > 700).isTrue();

        // following info is not contained in pocket query gpx file
        assertThat(cache.getAttributes()).isEmpty();
    }

    private static long parseTime(final String time) {
        try {
            return LOG_DATE_FORMAT.parse(time).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    private static void assertGc31j2hWaypoints(final Geocache cache) {
        assertThat(cache.getWaypoints()).isNotNull();
        assertThat(cache.getWaypoints()).hasSize(2);
        Waypoint wp = cache.getWaypoints().get(0);
        assertThat(wp.getGeocode()).isEqualTo("GC31J2H");
        assertThat(wp.getPrefix()).isEqualTo("00");
        assertThat(wp.getLookup()).isEqualTo("---");
        assertThat(wp.getName()).isEqualTo("Parkplatz");
        assertThat(wp.getNote()).isEqualTo("Kostenfreies Parken (je nach Parkreihe Parkscheibe erforderlich)");
        assertThat(wp.getWaypointType()).isEqualTo(WaypointType.PARKING);
        assertEquals(49.317517, wp.getCoords().getLatitude(), 0.000001);
        assertEquals(8.545083, wp.getCoords().getLongitude(), 0.000001);

        wp = cache.getWaypoints().get(1);
        assertThat(wp.getGeocode()).isEqualTo("GC31J2H");
        assertThat(wp.getPrefix()).isEqualTo("S1");
        assertThat(wp.getLookup()).isEqualTo("---");
        assertThat(wp.getName()).isEqualTo("Station 1");
        assertThat(wp.getNote()).isEqualTo("Ein zweiter Wegpunkt, der nicht wirklich existiert sondern nur zum Testen gedacht ist.");
        assertThat(wp.getWaypointType()).isEqualTo(WaypointType.STAGE);
        assertEquals(49.317500, wp.getCoords().getLatitude(), 0.000001);
        assertEquals(8.545100, wp.getCoords().getLongitude(), 0.000001);
    }

    private List<Geocache> readGPX10(int... resourceIds) throws IOException, ParserException {
        final GPX10Parser parser = new GPX10Parser(getTemporaryListId());
        return readVersionedGPX(parser, resourceIds);
    }

    private List<Geocache> readGPX11(int... resourceIds) throws IOException, ParserException {
        final GPX11Parser parser = new GPX11Parser(getTemporaryListId());
        return readVersionedGPX(parser, resourceIds);
    }

    private List<Geocache> readVersionedGPX(final GPXParser parser, int... resourceIds) throws IOException, ParserException {
        final Set<String> result = new HashSet<String>();
        for (int resourceId : resourceIds) {
            final InputStream instream = getResourceStream(resourceId);
            try {
                Collection<Geocache> caches = parser.parse(instream, null);
                assertThat(caches).isNotNull();
                for (Geocache cache : caches) {
                    result.add(cache.getGeocode());
                }
            } finally {
                instream.close();
            }
        }
        // reload caches, because the parser only returns the minimum version of each cache
        return new ArrayList<Geocache>(DataStore.loadCaches(result, LoadFlags.LOAD_ALL_DB_ONLY));
    }

    public static void testParseDateWithFractionalSeconds() throws ParseException {
        // was experienced in GSAK file
        final String dateString = "2011-08-13T02:52:18.103Z";
        GPXParser.parseDate(dateString);
    }

    public static void testParseDateWithHugeFraction() throws ParseException {
        // see issue 821
        final String dateString = "2011-11-07T00:00:00.0000000-07:00";
        GPXParser.parseDate(dateString);
    }

    public void testSelfmadeGPXWithoutGeocodes() throws Exception {
        final List<Geocache> caches = readGPX11(R.raw.no_connector);
        assertThat(caches).hasSize(13);
    }

    public void testTexasChallenge2012() throws Exception {
        final List<Geocache> caches = readGPX10(R.raw.challenge);
        // previously these caches overwrote each other during parsing
        assertThat(caches).hasSize(130);
    }

    public void testGeoToad() throws Exception {
        final List<Geocache> caches = readGPX10(R.raw.geotoad);
        assertThat(caches).hasSize(2);
        final List<String> codes = new ArrayList<String>();
        for (Geocache cache : caches) {
            codes.add(cache.getGeocode());
        }
        assertThat(codes.contains("GC2KN6K")).isTrue();
        assertThat(codes.contains("GC1T3MK")).isTrue();
    }

    public void testLazyLogLoading() throws IOException, ParserException {
        // this test should be in CacheTest, but it is easier to create here due to the GPX import abilities
        final String geocode = "GC31J2H";
        removeCacheCompletely(geocode);
        final List<Geocache> caches = readGPX10(R.raw.lazy);
        assertThat(caches).hasSize(1);
        DataStore.removeAllFromCache();
        // load only the minimum cache, it has several members missing
        final Geocache minimalCache = DataStore.loadCache(geocode, EnumSet.of(LoadFlag.DB_MINIMAL));

        // now check that we load lazy members on demand
        assertThat(minimalCache.getAttributes()).isNotEmpty();
        assertThat(minimalCache.getLogs()).isNotEmpty();

        removeCacheCompletely(geocode);
    }

    public void testDuplicateImport() throws IOException, ParserException {
        final String geocode = "GC31J2H";
        removeCacheCompletely(geocode);

        // first import
        List<Geocache> caches = readGPX10(R.raw.lazy);
        assertThat(caches).hasSize(1);
        assertThat(caches.get(0).getLogs()).hasSize(6);

        // second import
        caches = readGPX10(R.raw.lazy);
        assertThat(caches).hasSize(1);
        assertThat(caches.get(0).getLogs()).hasSize(6);

        removeCacheCompletely(geocode);
    }

    public void testWaymarking() throws Exception {
        final List<Geocache> caches = readGPX10(R.raw.waymarking_gpx);
        assertThat(caches).hasSize(1);
        final Geocache waymark = caches.get(0);
        assertThat(waymark).isNotNull();
        assertThat(waymark.getGeocode()).isEqualTo("WM7BM7");
        assertThat(waymark.getName()).isEqualTo("Roman water pipe Kornwestheim");
        assertThat(waymark.getUrl()).isNotEmpty(); // connector must be able to create it
        assertThat(waymark.getType()).isEqualTo(CacheType.UNKNOWN);
        assertThat(waymark.getSize()).isEqualTo(CacheSize.UNKNOWN);
    }

    /**
     * This one uses geocodes where the first character is actually a digit, not a character
     */
    public void testGCTour() throws Exception {
        final List<Geocache> caches = readGPX10(R.raw.gctour_gpx);
        assertThat(caches).hasSize(54);
    }

    public void testOX() throws IOException, ParserException {
        final List<Geocache> caches = readGPX10(R.raw.ox1ry0y_gpx);
        assertThat(caches).hasSize(1);
        final Geocache cache = caches.get(0);
        assertThat(cache.getGeocode()).isEqualTo("OX1RY0Y");
        assertThat(cache.getType()).isEqualTo(CacheType.TRADITIONAL);
        assertThat(cache.isArchived()).isEqualTo(false);
        assertThat(cache.isDisabled()).isEqualTo(false);
        assertThat(cache.getName()).isEqualTo("Kornwestheim und die Römer");
        assertThat(cache.getOwnerDisplayName()).isEqualTo("Thomas&Dani");
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL);
        assertThat(cache.getDifficulty()).isEqualTo(1.5f);
        assertThat(cache.getTerrain()).isEqualTo(1.0f);
        assertThat(cache.getDescription().startsWith("Dieses sind die Reste einer in Kornwestheim gefundenen")).isTrue();
        assertThat(cache.getCoords()).isEqualTo(new Geopoint(48.8642167, 9.1836));
        assertThat(cache.isReliableLatLon()).isTrue();
        assertThat(cache.getHint()).isEqualTo("Wasserleitung");
    }

    private Geocache getFirstCache(int gpxResourceId) throws IOException, ParserException {
        final List<Geocache> caches = readGPX10(gpxResourceId);
        assertThat(caches).isNotNull();
        assertThat(caches).hasSize(1);
        final Geocache cache = caches.get(0);
        return cache;
    }

    public void testGsakFavPoints() throws IOException, ParserException {
        final Geocache cache = getFirstCache(R.raw.gc3t1xg_gsak);
        assertThat(cache.getFavoritePoints()).isEqualTo(258);
    }

    public void testGsakPersonalNote() throws IOException, ParserException {
        final Geocache cache = getFirstCache(R.raw.gc3t1xg_gsak);
        assertThat(cache.getPersonalNote()).isEqualTo("Personal Note Test");
    }

    public void testGsakPremium() throws IOException, ParserException {
        final Geocache cache = getFirstCache(R.raw.gc3t1xg_gsak);
        assertThat(cache.isPremiumMembersOnly()).isTrue();
    }

    public void testGPXMysteryType() throws IOException, ParserException {
        final List<Geocache> caches = readGPX10(R.raw.tc2012);
        Geocache mystery = getCache(caches, "U017");
        assertThat(mystery).isNotNull();
        assert (mystery != null);
        assertThat(mystery.getType()).isEqualTo(CacheType.MYSTERY);
    }

    private static Geocache getCache(final List<Geocache> caches, String geocode) {
        for (Geocache geocache : caches) {
            if (geocache.getName().equals(geocode)) {
                return geocache;
            }
        }
        return null;
    }

    public void testLabCaches() throws IOException, ParserException {
        final List<Geocache> caches = readGPX10(R.raw.giga_lab_caches);
        assertThat(caches).hasSize(10);
        Geocache lab = getCache(caches, "01_Munich Olympic Walk Of Stars_Updated-Project MUNICH2014 - Mia san Giga! Olympiapark");
        assertThat(lab).isNotNull();

        // parse labs as virtual for the time being
        assertThat(lab.getType()).isEqualTo(CacheType.VIRTUAL);

        // no difficulty and terrain rating
        assertThat(lab.getTerrain()).isEqualTo(0);
        assertThat(lab.getDifficulty()).isEqualTo(0);

        // geocodes are just big hashes
        assertThat(lab.getGeocode()).isEqualTo("01_Munich Olympic Walk Of Stars_Updated-Project MUNICH2014 - Mia san Giga! Olympiapark".toUpperCase(Locale.US));

        // other normal cache properties
        assertThat(lab.getName()).isEqualTo("01_Munich Olympic Walk Of Stars_Updated-Project MUNICH2014 - Mia san Giga! Olympiapark");
        assertThat(lab.getShortDescription()).isEqualTo("01_Munich Olympic Walk Of Stars_Updated (Giga! Olympia Park)-Project MUNICH2014 - Mia san Giga! Olympiapark");
        assertThat(lab.getDescription()).startsWith("DEU:");
    }

}
