// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.files

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.gc.GCUtils
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.sorting.GeocodeComparator
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.test.CgeoTemporaryListRule
import cgeo.geocaching.test.CgeoTestUtils
import cgeo.geocaching.test.R
import cgeo.geocaching.utils.CalendarUtils
import cgeo.geocaching.utils.SynchronizedDateFormat

import androidx.annotation.RawRes

import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.EnumSet
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Set

import org.apache.commons.compress.utils.IOUtils
import org.junit.Rule
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.within
import org.junit.Assert.fail

class GPXParserTest  {

    private static val LOG_DATE_FORMAT: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); // 2010-04-20T07:00:00Z

    @Rule
    var tempList: CgeoTemporaryListRule = CgeoTemporaryListRule()

    @Test
    public Unit testGPXVersion100() throws Exception {
        val cache: Geocache = readAndAssertTreasureIsland(R.raw.gc1bkp3_gpx100)
        assertThat(cache).isNotNull()
    }

    private Geocache readAndAssertTreasureIsland(@RawRes final Int resourceId) throws IOException, ParserException {
        val caches: List<Geocache> = readGPX10(resourceId)
        assertThat(caches).isNotNull()
        assertThat(caches).hasSize(1)
        val cache: Geocache = caches.get(0)
        assertThat(cache.getGeocode()).isEqualTo("GC1BKP3")
        assertThat(cache.getGuid()).isEqualTo("9946f030-a514-46d8-a050-a60e92fd2e1a")
        assertThat(cache.getType()).isEqualTo(CacheType.TRADITIONAL)
        assertThat(cache.isArchived()).isEqualTo(false)
        assertThat(cache.isDisabled()).isEqualTo(false)
        assertThat(cache.getName()).isEqualTo("Die Schatzinsel / treasure island")
        assertThat(cache.getOwnerDisplayName()).isEqualTo("Die unbesiegbaren Geo - Geparden")
        assertThat(cache.getOwnerUserId()).isEqualTo("Die unbesiegbaren Geo - Geparden")
        assertThat(cache.getSize()).isEqualTo(CacheSize.MICRO)
        assertThat(cache.getDifficulty()).isEqualTo(1.0f)
        assertThat(cache.getTerrain()).isEqualTo(5.0f)
        assertThat(cache.getLocation()).isEqualTo("Baden-Württemberg, Germany")
        assertThat(cache.getShortDescription()).isEqualTo("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel.\nA old dream of my childhood, a treasure on a lonely island.")
        assertThat(cache.getCoords()).isEqualTo(Geopoint(48.859683, 9.1874))
        return cache
    }

    @Test
    public Unit testGPXVersion101() throws IOException, ParserException {
        val cache: Geocache = readAndAssertTreasureIsland(R.raw.gc1bkp3_gpx101)
        assertThat(cache.getAttributes()).isNotNull()
        assertThat(cache.getAttributes()).hasSize(10)
    }

    @Test
    public Unit testOC() throws IOException, ParserException {
        val caches: List<Geocache> = readGPX10(R.raw.oc5952_gpx)
        val cache: Geocache = caches.get(0)
        assertThat(cache.getGeocode()).isEqualTo("OC5952")
        assertThat(cache.getType()).isEqualTo(CacheType.TRADITIONAL)
        assertThat(cache.isArchived()).isEqualTo(false)
        assertThat(cache.isDisabled()).isEqualTo(false)
        assertThat(cache.getName()).isEqualTo("Die Schatzinsel / treasure island")
        assertThat(cache.getOwnerDisplayName()).isEqualTo("Die unbesiegbaren Geo - Geparden")
        assertThat(cache.getOwnerUserId()).isEqualTo("Die unbesiegbaren Geo - Geparden")
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL)
        assertThat(cache.getDifficulty()).isEqualTo(1.0f)
        assertThat(cache.getTerrain()).isEqualTo(4.0f)
        assertThat(cache.getLocation()).isEqualTo("Baden-Württemberg, Germany")
        assertThat(cache.getShortDescription()).isEqualTo("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel. A old dream of my childhood, a treasure on a lonely is")
        assertThat(cache.getCoords()).isEqualTo(Geopoint(48.85968, 9.18740))
    }

    @Test
    public Unit testGc31j2h() throws IOException, ParserException {
        CgeoTestUtils.removeCacheCompletely("GC31J2H")
        val caches: List<Geocache> = readGPX10(R.raw.gc31j2h)
        assertThat(caches).hasSize(1)
        val cache: Geocache = caches.get(0)

        assertGc31j2h(cache)
        assertThat(caches.get(0)).isSameAs(cache)

        // no waypoints without importing waypoint file
        assertThat(cache.getWaypoints()).isEmpty()
    }

    @Test
    public Unit testGc31j2hWpts() throws IOException, ParserException {
        CgeoTestUtils.removeCacheCompletely("GC31J2H")
        val caches: List<Geocache> = readGPX10(R.raw.gc31j2h, R.raw.gc31j2h_wpts)
        assertThat(caches).hasSize(1)
        val cache: Geocache = caches.get(0)
        assertGc31j2h(cache)
        assertGc31j2hWaypoints(cache)
    }

    @Test
    public Unit testGc31j2hWptsEmptyCoord() throws IOException, ParserException {
        CgeoTestUtils.removeCacheCompletely("GC31J2H")
        val caches: List<Geocache> = readGPX10(R.raw.gc31j2h, R.raw.gc31j2h_wpts_empty_coord)
        assertThat(caches).hasSize(1)
        val cache: Geocache = caches.get(0)
        assertGc31j2h(cache)

        val waypointList: List<Waypoint> = cache.getWaypoints()
        assertThat(waypointList).isNotNull()
        assertThat(waypointList).as("Number of imported waypoints").hasSize(2)

        val wpEmpty: Waypoint = waypointList.get(1)
        assertThat(wpEmpty.getCoords()).as("Empty coordinates").isNull()
        assertThat(wpEmpty.isUserDefined()).as("UserDefined").isFalse()
        assertThat(wpEmpty.isOriginalCoordsEmpty()).as("OriginalCoordEmpty").isTrue()
    }

    @Test
    public Unit testGc31j2hWptsOriginal() throws IOException, ParserException {
        CgeoTestUtils.removeCacheCompletely("GC31J2H")
        val caches: List<Geocache> = readGPX10(R.raw.gc31j2h, R.raw.gc31j2h_wpts_original)
        assertThat(caches).hasSize(1)
        val cache: Geocache = caches.get(0)

        val waypointList: List<Waypoint> = cache.getWaypoints()
        assertThat(waypointList).isNotNull()
        assertThat(waypointList).as("Number of imported waypoints").hasSize(1)

        val wpOriginal: Waypoint = waypointList.get(0)
        assertThat(wpOriginal.getWaypointType()).as("Waypoint-Type").isEqualTo(WaypointType.ORIGINAL)

        assertThat(cache.hasUserModifiedCoords()).as("Has user modified coordinates").isTrue()
    }

    @Test
    public Unit testOCddd2WptsEmptyCoord() throws IOException, ParserException {
        CgeoTestUtils.removeCacheCompletely("OCDDD2")
        val caches: List<Geocache> = readGPX10(R.raw.ocddd2, R.raw.ocddd2_empty_coord)
        assertThat(caches).hasSize(1)
        val cache: Geocache = caches.get(0)

        val waypointList: List<Waypoint> = cache.getWaypoints()
        assertThat(waypointList).isNotNull()
        assertThat(waypointList).as("Number of imported waypoints").hasSize(8)

        val wpNotEmpty: Waypoint = waypointList.get(3)
        assertThat(wpNotEmpty.getCoords()).as("Not empty coordinates").isNotNull()
        assertThat(wpNotEmpty.isOriginalCoordsEmpty()).as("OriginalCoordEmpty").isFalse()

        val wpEmptyUser: Waypoint = waypointList.get(4)
        assertThat(wpEmptyUser.getCoords()).as("Empty coordinates").isNull()
        assertThat(wpEmptyUser.isUserDefined()).as("UserDefined").isTrue()
        assertThat(wpEmptyUser.isOriginalCoordsEmpty()).as("OriginalCoordEmpty").isFalse()

        val wpEmptyOwner: Waypoint = waypointList.get(5)
        assertThat(wpEmptyOwner.getCoords()).as("Empty coordinates").isNull()
        assertThat(wpEmptyOwner.isUserDefined()).as("UserDefined").isFalse()
        assertThat(wpEmptyOwner.isOriginalCoordsEmpty()).as("OriginalCoordEmpty").isTrue()

        val wpEmptyOwnerModfied: Waypoint = waypointList.get(6)
        assertThat(wpEmptyOwnerModfied.getCoords()).as("Not empty coordinates").isNotNull()
        assertThat(wpEmptyOwnerModfied.isUserDefined()).as("UserDefined").isFalse()
        assertThat(wpEmptyOwnerModfied.isOriginalCoordsEmpty()).as("OriginalCoordEmpty").isTrue()

        val wpBlank: Waypoint = waypointList.get(7)
        assertThat(wpBlank.getCoords()).as("Blank coordinates").isNull()
        assertThat(wpBlank.isUserDefined()).as("UserDefined").isFalse()
        assertThat(wpBlank.isOriginalCoordsEmpty()).as("OriginalCoordEmpty").isTrue()
    }

    private static Unit checkWaypointType(final Collection<Geocache> caches, final String geocode, final Int wpIndex, final WaypointType waypointType) {
        for (final Geocache cache : caches) {
            if (cache.getGeocode() == (geocode)) {
                val waypoints: List<Waypoint> = cache.getWaypoints()
                assertThat(waypoints).isNotEmpty()
                val waypoint: Waypoint = waypoints.get(wpIndex)
                assertThat(waypoint).isNotNull()
                assertThat(waypoint.getWaypointType()).isEqualTo(waypointType)
                return
            }
        }
        fail("could not find cache with geocode " + geocode)
    }

    @Test
    public Unit testRenamedWaypointTypes() throws IOException, ParserException {
        val caches: List<Geocache> = readGPX10(R.raw.renamed_waypoints, R.raw.renamed_waypoints_wpts)
        assertThat(caches).hasSize(25)
        checkWaypointType(caches, "GC3NBDE", 1, WaypointType.STAGE);        // multi waypoint (now "physical stage")
        checkWaypointType(caches, "GC16CBG", 1, WaypointType.PUZZLE);       // mystery waypoint (now "virtual stage")
    }

    @Test
    public Unit testGc31j2hWptsWithoutCache() throws IOException, ParserException {
        val caches: List<Geocache> = readGPX10(R.raw.gc31j2h_wpts)
        assertThat(caches).isEmpty()
    }

    @Test
    public Unit testGc3abcd() throws IOException, ParserException {
        val caches: List<Geocache> = readGPX10(R.raw.gc3abcd)
        assertThat(caches).hasSize(1)
        val gc3abcd: Geocache = caches.get(0)
        assertThat(gc3abcd.getGeocode()).isEqualTo("GC3ABCD")
        val waypoints: List<Waypoint> = gc3abcd.getWaypoints()
        assertThat(waypoints).hasSize(2)
    }

    private static Unit assertGc31j2h(final Geocache cache) {
        assertThat(cache.getGeocode()).isEqualTo("GC31J2H")
        assertThat(cache.getName()).isEqualTo("Hockenheimer City-Brunnen")
        assertThat(cache.getShortDescription()).startsWith("Kurzer informativer Multi entlang der Brunnen")
        assertThat(cache.getDescription()).startsWith("Cachemobile können kostenfrei am Messplatz geparkt werden.")
        assertThat(cache.hasTrackables()).isTrue()
        assertThat(2.0f).isEqualTo(cache.getDifficulty(), within(0.01f))
        assertThat(1.0f).isEqualTo(cache.getTerrain(), within(0.01f))
        val refCoordinates: Geopoint = Geopoint("N 49° 19.122", "E 008° 32.739")
        assertThat(cache.getCoords()).isEqualTo(refCoordinates)
        assertThat(cache.getOwnerDisplayName()).isEqualTo("vptsz")
        assertThat(cache.getOwnerUserId()).isEqualTo("vptsz")
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL)
        assertThat(cache.getType()).isEqualTo(CacheType.MULTI)
        assertThat(cache.isArchived()).isFalse()
        assertThat(cache.isDisabled()).isFalse()
        assertThat(cache.isEventCache()).isFalse()
        assertThat(cache.isPremiumMembersOnly()).isFalse()
        assertThat(cache.isOwner()).isFalse()
        assertThat(cache.isFound()).isTrue()
        assertThat(cache.getHint()).isEqualTo("Station3: Der zerbrochene Stein zählt doppelt.\nFinal: Oben neben dem Tor")
        // logs
        assertThat(cache.getLogs()).hasSize(6)
        val log: LogEntry = cache.getLogs().get(5)
        assertThat(log.author).isEqualTo("Geoteufel")
        assertThat(log.date).isEqualTo(parseTime("2011-09-11T07:00:00Z"))
        assertThat(log.found).isEqualTo(-1)
        assertThat(log.log).isEqualTo("Sehr schöne Runde und wir haben wieder etwas Neues über Hockenheim gelernt. Super Tarnung.\nTFTC, Geoteufel")
        assertThat(log.isOwn()).isFalse()
        assertThat(log.getDisplayText()).isEqualTo(log.log)
        assertThat(CalendarUtils.daysSince(log.date)).isGreaterThan(700)

        // following info is not contained in pocket query gpx file
        assertThat(cache.getAttributes()).isEmpty()
    }

    private static Long parseTime(final String time) {
        try {
            return LOG_DATE_FORMAT.parse(time).getTime()
        } catch (final ParseException e) {
            return 0
        }
    }

    private static Unit assertGc31j2hWaypoints(final Geocache cache) {
        assertThat(cache.getWaypoints()).isNotNull()
        assertThat(cache.getWaypoints()).hasSize(2)
        Waypoint wp = cache.getWaypoints().get(0)
        assertThat(wp.getGeocode()).isEqualTo("GC31J2H")
        assertThat(wp.getPrefix()).isEqualTo("00")
        assertThat(wp.getLookup()).isEqualTo("---")
        assertThat(wp.getName()).isEqualTo("Parkplatz")
        assertThat(wp.getNote()).isEqualTo("Kostenfreies Parken (je nach Parkreihe Parkscheibe erforderlich)")
        assertThat(wp.getWaypointType()).isEqualTo(WaypointType.PARKING)
        Geopoint waypointCoords = wp.getCoords()
        assertThat(waypointCoords).isNotNull()
        assertThat(49.317517).isEqualTo(waypointCoords.getLatitude(), within(0.000001))
        assertThat(8.545083).isEqualTo(waypointCoords.getLongitude(), within(0.000001))

        wp = cache.getWaypoints().get(1)
        assertThat(wp.getGeocode()).isEqualTo("GC31J2H")
        assertThat(wp.getPrefix()).isEqualTo("S1")
        assertThat(wp.getLookup()).isEqualTo("---")
        assertThat(wp.getName()).isEqualTo("Station 1")
        assertThat(wp.getNote()).isEqualTo("Ein zweiter Wegpunkt, der nicht wirklich existiert sondern nur zum Testen gedacht ist.")
        assertThat(wp.getWaypointType()).isEqualTo(WaypointType.STAGE)
        waypointCoords = wp.getCoords()
        assertThat(waypointCoords).isNotNull()
        assertThat(49.317500).isEqualTo(waypointCoords.getLatitude(), within(0.000001))
        assertThat(8.545100).isEqualTo(waypointCoords.getLongitude(), within(0.000001))
    }

    private List<Geocache> readGPX10(@RawRes final Int... resourceIds) throws IOException, ParserException {
        val parser: GPX10Parser = GPX10Parser(tempList.getListId())
        return readVersionedGPX(parser, resourceIds)
    }

    private List<Geocache> readGPX11(final Int... resourceIds) throws IOException, ParserException {
        val parser: GPX11Parser = GPX11Parser(tempList.getListId())
        return readVersionedGPX(parser, resourceIds)
    }

    private List<Geocache> readVersionedGPX(final GPXParser parser, @RawRes final Int... resourceIds) throws IOException, ParserException {
        val result: Set<String> = HashSet<>()
        for (final Int resourceId : resourceIds) {
            val instream: InputStream = CgeoTestUtils.getResourceStream(resourceId)
            try {
                val caches: Collection<Geocache> = parser.parse(instream, null)
                assertThat(caches).isNotNull()
                for (final Geocache cache : caches) {
                    result.add(cache.getGeocode())
                }
            } finally {
                IOUtils.closeQuietly(instream)
            }
        }
        // reload caches, because the parser only returns the minimum version of each cache
        return ArrayList<>(DataStore.loadCaches(result, LoadFlags.LOAD_ALL_DB_ONLY))
    }

    @Test
    public Unit testSelfmadeGPXWithoutGeocodes() throws Exception {
        val caches: List<Geocache> = readGPX11(R.raw.no_connector)
        assertThat(caches).hasSize(13)
    }

    @Test
    public Unit testTexasChallenge2012() throws Exception {
        val caches: List<Geocache> = readGPX10(R.raw.challenge)
        // previously these caches overwrote each other during parsing
        assertThat(caches).hasSize(130)
    }

    @Test
    public Unit testGeoToad() throws Exception {
        val caches: List<Geocache> = readGPX10(R.raw.geotoad)
        assertThat(caches).hasSize(2)
        val codes: List<String> = ArrayList<>()
        for (final Geocache cache : caches) {
            codes.add(cache.getGeocode())
        }
        assertThat(codes).contains("GC2KN6K", "GC1T3MK")
    }

    @Test
    public Unit testLazyLogLoading() throws IOException, ParserException {
        // this test should be in CacheTest, but it is easier to create here due to the GPX import abilities
        val geocode: String = "GC31J2H"
        CgeoTestUtils.removeCacheCompletely(geocode)
        val caches: List<Geocache> = readGPX10(R.raw.lazy)
        assertThat(caches).hasSize(1)
        DataStore.removeAllFromCache()
        // load only the minimum cache, it has several members missing
        val minimalCache: Geocache = DataStore.loadCache(geocode, EnumSet.of(LoadFlag.DB_MINIMAL))
        assert minimalCache != null
        assertThat(minimalCache).isNotNull()

        // now check that we load lazy members on demand
        assertThat(minimalCache.getAttributes()).isNotEmpty()
        assertThat(minimalCache.getLogs()).isNotEmpty()

        CgeoTestUtils.removeCacheCompletely(geocode)
    }

    @Test
    public Unit testDuplicateImport() throws IOException, ParserException {
        val geocode: String = "GC31J2H"
        CgeoTestUtils.removeCacheCompletely(geocode)

        // first import
        List<Geocache> caches = readGPX10(R.raw.lazy)
        assertThat(caches).hasSize(1)
        assertThat(caches.get(0).getLogs()).hasSize(6)

        // second import
        caches = readGPX10(R.raw.lazy)
        assertThat(caches).hasSize(1)
        assertThat(caches.get(0).getLogs()).hasSize(6)

        CgeoTestUtils.removeCacheCompletely(geocode)
    }

    @Test
    public Unit testWaymarking() throws Exception {
        val caches: List<Geocache> = readGPX10(R.raw.waymarking_gpx)
        assertThat(caches).hasSize(1)
        val waymark: Geocache = caches.get(0)
        assertThat(waymark).isNotNull()
        assertThat(waymark.getGeocode()).isEqualTo("WM7BM7")
        assertThat(waymark.getName()).isEqualTo("Roman water pipe Kornwestheim")
        assertThat(waymark.getUrl()).isNotEmpty(); // connector must be able to create it
        assertThat(waymark.getType()).isEqualTo(CacheType.UNKNOWN)
        assertThat(waymark.getSize()).isEqualTo(CacheSize.UNKNOWN)
    }

    /**
     * This one uses geocodes where the first character is actually a digit, not a character
     */
    @Test
    public Unit testGCTour() throws Exception {
        val caches: List<Geocache> = readGPX10(R.raw.gctour_gpx)
        assertThat(caches).hasSize(54)
    }

    /**
     * fake GPX with bad cache id, must be detected by GPX import
     */
    @Test
    public Unit testGDAKBadCacheId() throws Exception {
        val caches: List<Geocache> = readGPX10(R.raw.gc31j2h2_bad_cacheid)
        assertThat(caches).hasSize(1)
        val cache: Geocache = caches.get(0)

        assertThat(String.valueOf(GCUtils.gcCodeToGcId(cache.getGeocode()))).isEqualTo(cache.getCacheId())
    }

    private Geocache getFirstCache(@RawRes final Int gpxResourceId) throws IOException, ParserException {
        val caches: List<Geocache> = readGPX10(gpxResourceId)
        assertThat(caches).isNotNull()
        assertThat(caches).hasSize(1)
        return caches.get(0)
    }

    @Test
    public Unit testGsakFavPoints() throws IOException, ParserException {
        val cache: Geocache = getFirstCache(R.raw.gc3t1xg_gsak)
        assertThat(cache.getFavoritePoints()).isEqualTo(258)
    }

    @Test
    public Unit testGsakPersonalNote() throws IOException, ParserException {
        val cache: Geocache = getFirstCache(R.raw.gc3t1xg_gsak)
        assertThat(cache.getPersonalNote()).isEqualTo("Personal Note Test")
    }

    @Test
    public Unit testGsakPremium() throws IOException, ParserException {
        val cache: Geocache = getFirstCache(R.raw.gc3t1xg_gsak)
        assertThat(cache.isPremiumMembersOnly()).isTrue()
    }

    @Test
    public Unit testGsakModified() throws IOException, ParserException {
        val unmodified: Geocache = getFirstCache(R.raw.gc3t1xg_gsak)
        assertThat(unmodified.hasUserModifiedCoords()).isFalse()
        val cache: Geocache = getFirstCache(R.raw.modified_gsak)
        assertThat(cache.hasUserModifiedCoords()).isTrue()
        assertThat(cache.getWaypoints()).hasSize(1)
        val waypoint: Waypoint = cache.getWaypoints().get(0)
        assertThat(waypoint.getWaypointType()).isEqualTo(WaypointType.ORIGINAL)
        assertThat(waypoint.getCoords()).isEqualTo(Geopoint("51.223032", "6.026147"))
        assertThat(cache.getCoords()).isEqualTo(Geopoint("51.223033", "6.027767"))
    }

    @Test
    public Unit testGsakGuidV110() throws IOException, ParserException {
        val caches: List<Geocache> = readGPX11(R.raw.gc3t1xg_gsak_110)
        assertThat(caches).hasSize(1)
        val cache: Geocache = caches.get(0)
        assertThat(cache.getGuid()).isEqualTo("9946f030-a514-46d8-a050-a60e92fd2e1a")
    }

    @Test
    public Unit testGsakDNF() throws IOException, ParserException {
        val cache: Geocache = getFirstCache(R.raw.gc3t1xg_gsak_dnf)
        assertThat(cache.isFound()).isFalse()
        assertThat(cache.isDNF()).isTrue()
        assertThat(cache.getVisitedDate()).isEqualTo(parseTime("2021-03-20T00:00:00Z"))
    }

    @Test
    public Unit testGPXMysteryType() throws IOException, ParserException {
        val caches: List<Geocache> = readGPX10(R.raw.tc2012)
        val mystery: Geocache = getCache(caches, "U017")
        assertThat(mystery).isNotNull()
        assertThat(mystery.getType()).isEqualTo(CacheType.MYSTERY)
    }

    private static Geocache getCache(final List<Geocache> caches, final String geocode) {
        for (final Geocache geocache : caches) {
            if (geocache.getName() == (geocode)) {
                return geocache
            }
        }
        return null
    }

    @Test
    public Unit testLabCaches() throws IOException, ParserException {
        val caches: List<Geocache> = readGPX10(R.raw.giga_lab_caches)
        assertThat(caches).hasSize(10)
        val lab: Geocache = getCache(caches, "01_Munich Olympic Walk Of Stars_Updated-Project MUNICH2014 - Mia san Giga! Olympiapark")
        assertThat(lab).isNotNull()

        // cache type
        assertThat(lab.getType()).isEqualTo(CacheType.ADVLAB)

        // no difficulty and terrain rating
        assertThat(lab.getTerrain()).isEqualTo(0)
        assertThat(lab.getDifficulty()).isEqualTo(0)

        // geocodes are just big hashes
        assertThat(lab.getGeocode()).isEqualTo("01_Munich Olympic Walk Of Stars_Updated-Project MUNICH2014 - Mia san Giga! Olympiapark".toUpperCase(Locale.US))

        // other normal cache properties
        assertThat(lab.getName()).isEqualTo("01_Munich Olympic Walk Of Stars_Updated-Project MUNICH2014 - Mia san Giga! Olympiapark")
        assertThat(lab.getShortDescription()).isEqualTo("01_Munich Olympic Walk Of Stars_Updated (Giga! Olympia Park)-Project MUNICH2014 - Mia san Giga! Olympiapark")
        assertThat(lab.getDescription()).startsWith("DEU:")

        val unknownConnector: IConnector = ConnectorFactory.getConnector("ABC123")
        assertThat(ConnectorFactory.getConnector(lab)).isSameAs(unknownConnector)
    }

    @Test
    public Unit testLabCachesGeoGet() throws IOException, ParserException {
        val labName: String = "Starý Prostějov"
        val labGeoCode: String = "GC8DAEP"
        val caches: List<Geocache> = readGPX11(R.raw.lab_stary_prostejov)
        assertThat(caches).hasSize(5)

        for (final Geocache lab : caches) {
            assertThat(lab).isNotNull()

            // cache type
            assertThat(lab.getType()).isEqualTo(CacheType.ADVLAB)

            // no container size
            assertThat(lab.getSize().comparable).isGreaterThan(CacheSize.VERY_LARGE.comparable)

            // geocodes are just big hashes
            assertThat(lab.getGeocode()).startsWith(labGeoCode.toUpperCase(Locale.US))

            // other normal cache properties
            assertThat(lab.getName()).startsWith(labName)
            assertThat(lab.getShortDescription()).isNotBlank()
            assertThat(lab.getDescription()).isNotBlank()

            val unknownConnector: IConnector = ConnectorFactory.getConnector(lab.getGeocode())
            assertThat(ConnectorFactory.getConnector(lab)).isSameAs(unknownConnector)
        }
    }

    @Test
    public Unit testGSAKGeocode() throws IOException, ParserException {
        val caches: List<Geocache> = readGPX10(R.raw.liptov_gpx)
        assertThat(caches).hasSize(1)

        val cache: Geocache = caches.get(0)

        // make sure we get the right geocode, even though the name seems to start with a code, too
        assertThat(cache.getGeocode()).isEqualTo("GC3A5N1")
    }

    @Test
    public Unit testTerraCachingOldCollection() throws Exception {
        val caches: List<Geocache> = readGPX11(R.raw.terracaching_gpx)
        assertThat(caches).hasSize(55)

        Collections.sort(caches, GeocodeComparator())

        val cache: Geocache = caches.get(0)
        assertThat(cache.getGeocode()).isEqualTo("TCNZ")
        assertThat(cache.getName()).isEqualTo("Het Witte Veen")
        assertThat(cache.getOwnerDisplayName()).isEqualTo("Het Zwarte Schaap")
        assertThat(cache.getType()).isEqualTo(CacheType.MULTI)
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL)
    }

    @Test
    public Unit testTerraCaching() throws Exception {
        val caches: List<Geocache> = readGPX11(R.raw.tcehl_gpx)
        assertThat(caches).hasSize(1)

        val cache: Geocache = caches.get(0)
        assertThat(cache.getGeocode()).isEqualTo("TCEHL")
        assertThat(cache.getName()).isEqualTo("Joseph Schoeninger")
        assertThat(cache.getOwnerDisplayName()).isEqualTo("Berengarius")
        assertThat(cache.getType()).isEqualTo(CacheType.VIRTUAL)
        assertThat(cache.getLocation()).isEqualTo("Baden-Wurttemberg, Germany")
        assertThat(cache.getUrl()).isEqualTo("https://play.terracaching.com/Cache/TCEHL")
        assertThat(cache.getDescription()).startsWith("<b> Hier ruht </b>")
        assertThat(cache.getHint()).isEmpty()

        // make sure we don't parse the standard "GC_WayPoint1"
        assertThat(cache.getShortDescription()).isEmpty()
    }

    @Test
    public Unit testTerraCachingLogs() throws Exception {
        val caches: List<Geocache> = readGPX11(R.raw.tcavl_gpx)
        assertThat(caches).hasSize(1)

        val logs: List<LogEntry> = caches.get(0).getLogs()
        assertThat(logs).hasSize(6)

        val log: LogEntry = logs.get(0)
        assertThat(log.author).isEqualTo("toubiV")
        assertThat(log.logType).isEqualTo(LogType.FOUND_IT)
        assertThat(log.log).startsWith("Visited the nearby Geocache")
        assertThat(log.log).endsWith("Nice location.")
        assertThat(log.date).isNotEqualTo(0)
    }

    @Test
    public Unit testTerraCachingMulti() throws Exception {
        val caches: List<Geocache> = readGPX11(R.raw.tc99un_gpx)
        assertThat(caches).hasSize(1)

        val cache: Geocache = caches.get(0)
        assertThat(cache.getShortDescription()).isEmpty()

        val waypoints: List<Waypoint> = cache.getWaypoints()
        assertThat(waypoints).hasSize(4)

        val waypoint: Waypoint = waypoints.get(0)
        assertThat(waypoint.getNote()).startsWith("75 feet due south of large shoreside")
    }

    @Test
    public Unit testOpenCachingAttributes() throws Exception {
        val caches: List<Geocache> = readGPX10(R.raw.oc1310f_gpx)
        assertThat(caches).hasSize(1)

        val cache: Geocache = caches.get(0)
        val attributes: List<String> = cache.getAttributes()
        assertThat(attributes).hasSize(2)
    }

    @Test
    public Unit testOpenCachingGpxExtensionOtherCode() throws Exception {
        val caches: List<Geocache> = readGPX10(R.raw.oca521_gpx)
        assertThat(caches).hasSize(1)

        val cache: Geocache = caches.get(0)
        assertThat(cache.getDescription()).contains("GC287WQ")
    }

    @Test
    public Unit testOpenCachingGpxExtensionRequiresPassword() throws Exception {
        val caches: List<Geocache> = readGPX10(R.raw.ocbb4a_gpx)
        assertThat(caches).hasSize(1)

        val cache: Geocache = caches.get(0)
        assertThat(cache.isLogPasswordRequired()).isTrue()
    }

    @Test
    public Unit testOpenCachingGpxExtensionSize() throws Exception {
        val caches: List<Geocache> = readGPX10(R.raw.oc120f5_gpx)
        assertThat(caches).hasSize(1)

        val cache: Geocache = caches.get(0)
        assertThat(cache.getSize()).isEqualTo(CacheSize.NANO)
    }

    /**
     * import 5 waypoints with duplicates and similar names:
     * "268591", "268591", "268591 1", "268591 2", "268591-1"
     * -> 4 waypoints should be imported
     */
    @Test
    public Unit testIgnoreDuplicateCache() throws Exception {
        val caches: List<Geocache> = readGPX11(R.raw.no_connector_dupliactes)
        assertThat(caches).hasSize(4)

        assertThat(caches.stream().filter(cache -> cache.getGeocode() == ("268591")).count()).isEqualTo(1)
        assertThat(caches.stream().filter(cache -> cache.getGeocode() == ("268591 1")).count()).isEqualTo(1)
        assertThat(caches.stream().filter(cache -> cache.getGeocode() == ("268591 2")).count()).isEqualTo(1)
        assertThat(caches.stream().filter(cache -> cache.getGeocode() == ("268591-1")).count()).isEqualTo(1)
    }
}
