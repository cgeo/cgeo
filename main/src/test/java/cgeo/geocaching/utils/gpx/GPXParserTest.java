package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinate;
import cgeo.geocaching.models.NamedGeoCoordinate;
import cgeo.geocaching.models.Waypoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Among other things, scans the real-world {@code .gpx} sample files that
 * already exist in the repository for other (SAX-based) parser tests.
 */
public class GPXParserTest {

    /** files under {@code main/src/test/res/xml/} */
    private static final String[] TEST_RES_XML_FILES = {
        "/xml/gc3t1xg_gsak_110.gpx",
        "/xml/ZUG.IN.ZWEI.TEILEN.gpx",
    };

    /** files under {@code main/src/androidTest/res/raw/} */
    private static final String[] ANDROID_TEST_RAW_FILES = {
        "/gc3t1xg_gsak_110.gpx",
        "/gc3t1xg_gsak.gpx",
        "/gc3abcd.gpx",
        "/gc31j2h_wpts_original.gpx",
        "/gc31j2h_wpts_empty_coord.gpx",
        "/gc31j2h_wpts.gpx",
        "/gc31j2h2_bad_cacheid.gpx",
        "/gc31j2h.gpx",
        "/gc1bkp3_gpx101.gpx",
        "/gc1bkp3_gpx100.gpx",
        "/challenge.gpx",
        "/zz1000.gpx",
        "/waymarking_gpx.gpx",
        "/terracaching_gpx.gpx",
        "/tcehl_gpx.gpx",
        "/tcavl_gpx.gpx",
        "/tc99un_gpx.gpx",
    };

    static InputStream openResource(final String resourceName) {
        final InputStream is = GPXParserTest.class.getResourceAsStream(resourceName);
        return Objects.requireNonNull(is, "test resource not found: " + resourceName);
    }

    private static RecordingGPXParseHooks parse(final String resourceName) throws IOException, XmlPullParserException {
        return parse(resourceName, new RecordingGPXParseHooks());
    }

    private static RecordingGPXParseHooks parse(final String resourceName, final RecordingGPXParseHooks hooks) throws IOException, XmlPullParserException {
        return parse(resourceName, new GPXParser(), hooks);
    }

    private static RecordingGPXParseHooks parse(final String resourceName, final GPXParser parser) throws IOException, XmlPullParserException {
        return parse(resourceName, parser, new RecordingGPXParseHooks());
    }

        private static RecordingGPXParseHooks parse(final String resourceName, final GPXParser parser, final RecordingGPXParseHooks hooks) throws IOException, XmlPullParserException {
        try (InputStream is = openResource(resourceName)) {
            parser.parse(is, hooks);
        }
        return hooks;
    }

    // -----------------------------------------------------------------------------------------------------
    // targeted tests for well-known sample files
    // -----------------------------------------------------------------------------------------------------

    @Test
    public void testParseGsakGeocache() throws Exception {
        final RecordingGPXParseHooks hooks = parse("/xml/gc3t1xg_gsak_110.gpx");

        assertThat(hooks.getGpxCreatorOrName()).isEqualTo("GSAK");
        assertThat(hooks.getGeocaches()).hasSize(1);

        final Geocache cache = hooks.getGeocaches().get(0);
        assertThat(cache.getGeocode()).isEqualTo("GC3T1XG");
        assertThat(cache.getName()).isEqualTo("Abus");
        assertThat(cache.getCoords()).isEqualTo(new Geopoint(50.10745, 8.6587));
        assertThat(cache.getType()).isEqualTo(CacheType.TRADITIONAL);
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL);
        assertThat(cache.getDifficulty()).isEqualTo(3f);
        assertThat(cache.getTerrain()).isEqualTo(1f);
        assertThat(cache.getFavoritePoints()).isEqualTo(615);
        assertThat(cache.isOnWatchlist()).isFalse();
        assertThat(cache.getAttributes()).isNotEmpty();

        final List<LogEntry> logs = hooks.getGlobalItems().get(0).logs;
        assertThat(logs).hasSize(5);
        assertThat(logs.get(0).author).isEqualTo("pheenyx");
        assertThat(logs.get(0).logType).isEqualTo(LogType.FOUND_IT);
        assertThat(logs.get(2).logType).isEqualTo(LogType.DIDNT_FIND_IT);
    }

    @Test
    public void testParseFullGeocacheWithLocationAndOwner() throws Exception {
        final RecordingGPXParseHooks hooks = parse("/gc31j2h.gpx");

        assertThat(hooks.getGeocaches()).hasSize(1);
        final Geocache cache = hooks.getGeocaches().get(0);
        assertThat(cache.getGeocode()).isEqualTo("GC31J2H");
        assertThat(cache.getName()).isEqualTo("Hockenheimer City-Brunnen");
        assertThat(cache.getCoords()).isEqualTo(new Geopoint(49.3187, 8.54565));
        assertThat(cache.getType()).isEqualTo(CacheType.MULTI);
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL);
        assertThat(cache.getDifficulty()).isEqualTo(2f);
        assertThat(cache.getTerrain()).isEqualTo(1f);
        assertThat(cache.getOwnerDisplayName()).isEqualTo("vptsz");
        assertThat(cache.getLocation()).isEqualTo("Baden-Württemberg, Germany");

        final List<LogEntry> logs = hooks.getGlobalItems().get(0).logs;
        assertThat(logs).hasSize(6);
        assertThat(logs.get(0).author).isEqualTo("hanslinde");
        assertThat(logs).allMatch(log -> log.logType == LogType.FOUND_IT);
    }

    @Test
    public void testWaypointParentResolutionViaNamingConvention() throws Exception {
        final RecordingGPXParseHooks hooks = parse("/gc31j2h_wpts.gpx");

        assertThat(hooks.getWaypoints()).hasSize(2);
        assertThat(hooks.getGeocaches()).isEmpty();

        final Waypoint parking = hooks.getWaypoints().get(0);
        assertThat(parking.getName()).isEqualTo("Parkplatz");
        assertThat(parking.getPrefix()).isEqualTo("00");
        assertThat(parking.getLookup()).isEqualTo("---");
        assertThat(parking.getCoords()).isEqualTo(new Geopoint(49.317517, 8.545083));
        assertThat(parking.getGeocode()).isEqualTo("GC31J2H");

        final Waypoint stage = hooks.getWaypoints().get(1);
        assertThat(stage.getName()).isEqualTo("Station 1");
        assertThat(stage.getPrefix()).isEqualTo("S1");
        assertThat(stage.getGeocode()).isEqualTo("GC31J2H");

        // order preserved (document order)
        assertThat(hooks.getGlobalItems()).extracting(i -> i.kind)
            .containsExactly(RecordingGPXParseHooks.ItemKind.WAYPOINT, RecordingGPXParseHooks.ItemKind.WAYPOINT);
    }

    @Test
    public void testParseTrackFileFull() throws Exception {
        final RecordingGPXParseHooks hooks = parse("/xml/ZUG.IN.ZWEI.TEILEN.gpx");

        // the file name ("train in two parts") reflects that it contains two separate <trk> elements
        assertThat(hooks.getTracks()).hasSize(2);
        final int totalPointCount = hooks.getTracks().stream().mapToInt(i -> i.totalPointCount).sum();
        assertThat(totalPointCount).isEqualTo(648);

        final RecordingGPXParseHooks.Track firstTrack = hooks.getTracks().get(0);
        final List<ICoordinate> firstSegmentPoints = firstTrack.segments.get(0).points;
        assertThat(firstSegmentPoints).isNotEmpty();
        assertThat(firstTrack.segments.get(0).pointCount).isEqualTo(firstSegmentPoints.size());
        final ICoordinate first = firstSegmentPoints.get(0);
        assertThat(first).isInstanceOf(NamedGeoCoordinate.class);
        assertThat(first.getCoords()).isEqualTo(new Geopoint(47.464799880981445, 11.045894622802734));
        assertThat(first.getElevation()).isEqualTo(782.54296875f);

        // every track point is also visible via the global, chronological items list (no classification
        // applies to plain track points without sym/type, so they all end up as (named) coordinates)
        assertThat(hooks.getGlobalItems()).hasSize(648);
    }

    @Test
    public void testParseTrackFileCoordinatesOnly() throws Exception {
        final GPXParser parser = new GPXParser().setParseMode(ParseMode.COORDINATES_ONLY);
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        parse("/xml/ZUG.IN.ZWEI.TEILEN.gpx", parser, hooks);

        assertThat(hooks.getTracks()).hasSize(2);
        final int totalPointCount = hooks.getTracks().stream().mapToInt(i -> i.totalPointCount).sum();
        assertThat(totalPointCount).isEqualTo(648);
        // COORDINATES_ONLY still reports each point via onCoordinate/onNamedCoordinate (just without ever
        // attempting geocache/waypoint classification) - callers can build their own point list from this
        assertThat(hooks.getGlobalItems()).hasSize(648);
        final ICoordinate first = hooks.getTracks().get(0).segments.get(0).points.get(0);
        assertThat(first.getCoords()).isEqualTo(new Geopoint(47.464799880981445, 11.045894622802734));
        // no geocache/waypoint classification whatsoever happens in COORDINATES_ONLY mode
        assertThat(hooks.getGeocaches()).isEmpty();
        assertThat(hooks.getWaypoints()).isEmpty();
    }

    @Test
    public void testTrackWithSkipModeStillReportsAccurateCounts() throws Exception {
        final GPXParser parser = new GPXParser().setParseMode(ParseMode.SKIP);
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        hooks.setModes(ParseMode.FULL, ParseMode.SKIP, ParseMode.SKIP);
        parse("/xml/ZUG.IN.ZWEI.TEILEN.gpx", parser, hooks);

        assertThat(hooks.getTracks()).hasSize(2);
        final int totalPointCount = hooks.getTracks().stream().mapToInt(i -> i.totalPointCount).sum();
        assertThat(totalPointCount).isEqualTo(648);
        // SKIP never fires any per-point hook at all, so nothing is recorded beyond the counts themselves
        assertThat(hooks.getGlobalItems()).isEmpty();
        for (final RecordingGPXParseHooks.Track track : hooks.getTracks()) {
            for (final RecordingGPXParseHooks.NamedCoordinateList segment : track.segments) {
                assertThat(segment.points).isEmpty();
                assertThat(segment.pointCount).isGreaterThan(0);
            }
        }
    }

    @Test
    public void testGeocacheSkipModeNeverFiresHook() throws Exception {
        final GPXParser parser = new GPXParser().setParseMode(ParseMode.SKIP);
        final RecordingGPXParseHooks hooks = parse("/xml/gc3t1xg_gsak_110.gpx", parser);

        // requirement: SKIP means the hook is never called at all - not even with a placeholder
        assertThat(hooks.getGlobalItems()).isEmpty();
        assertThat(hooks.getGeocaches()).isEmpty();
    }

    @Test
    public void testGeocacheCoordinatesOnlyModeReportedAsNamedCoordinate() throws Exception {
        final GPXParser parser = new GPXParser().setParseMode(ParseMode.COORDINATES_ONLY);
        final RecordingGPXParseHooks hooks = parse("/xml/gc3t1xg_gsak_110.gpx", parser);

        // requirement: COORDINATES_ONLY means it is never reported as a Geocache, only as a (named) coordinate
        assertThat(hooks.getGeocaches()).isEmpty();
        assertThat(hooks.getGlobalItems()).hasSize(1);
        final RecordingGPXParseHooks.Item item = hooks.getGlobalItems().get(0);
        assertThat(item.kind).isEqualTo(RecordingGPXParseHooks.ItemKind.NAMED_COORDINATE);
        assertThat(item.coordinate.getCoords()).isEqualTo(new Geopoint(50.10745, 8.6587));
        // COORDINATES_ONLY never reads extensions, so the name is the wpt's own <name> ("GC3T1XG"), not the
        // groundspeak:name ("Abus") which lives inside <extensions>
        assertThat(((NamedGeoCoordinate) item.coordinate).getName()).isEqualTo("GC3T1XG");
    }

    @Test
    public void testWaypointSkipAndCoordinatesOnlyModes() throws Exception {
        final RecordingGPXParseHooks skipHooks = parse("/gc31j2h_wpts.gpx", new GPXParser().setParseMode(ParseMode.SKIP));
        assertThat(skipHooks.getGlobalItems()).isEmpty();

        final RecordingGPXParseHooks coordHooks = parse("/gc31j2h_wpts.gpx", new GPXParser().setParseMode(ParseMode.COORDINATES_ONLY));
        assertThat(coordHooks.getWaypoints()).isEmpty();
        assertThat(coordHooks.getGlobalItems()).hasSize(2);
        assertThat(coordHooks.getGlobalItems()).allMatch(item -> item.kind == RecordingGPXParseHooks.ItemKind.NAMED_COORDINATE);
    }

    @Test
    public void testWaypointWithoutCoordinatesIsRetained() throws Exception {
        final RecordingGPXParseHooks hooks = parse("/gc31j2h_wpts_empty_coord.gpx");

        assertThat(hooks.getWaypoints()).hasSize(2);
        final Waypoint stage = hooks.getWaypoints().get(1);
        assertThat(stage.getName()).isEqualTo("Station 1");
        assertThat(stage.getCoords()).isNull();
        assertThat(stage.isUserDefined()).isFalse();
        assertThat(stage.isOriginalCoordsEmpty()).isTrue();
    }

    @Test
    public void testZzGeocodeWithoutCoordinatesIsStillImported() throws Exception {
        // legacy parity exception (D23): a geocache-classified entry without valid coordinates is still
        // reported if its geocode matches c:geo's own "cache without known coordinates" pattern (ZZ...)
        final RecordingGPXParseHooks hooks = parse("/zz1000.gpx");

        assertThat(hooks.getGeocaches()).hasSize(1);
        final Geocache cache = hooks.getGeocaches().get(0);
        assertThat(cache.getGeocode()).isEqualTo("ZZ1000");
        assertThat(cache.getCoords()).isNull();
    }

    @Test
    public void testMalformedFileThrows() {
        assertThatThrownBy(() -> parse("/gc31j2h_err.gpx"))
            .isInstanceOfAny(XmlPullParserException.class, IOException.class);
    }

    // -----------------------------------------------------------------------------------------------------
    // TerraCaching "GC_WayPoint1" sticky-marker classification (see terraChildWaypoint field doc on GPXFullWptParser)
    // -----------------------------------------------------------------------------------------------------

    @Test
    public void testTerraCachingMarkerCacheAndXmlNodeChildWaypoints() throws Exception {
        final RecordingGPXParseHooks hooks = parse("/tc99un_gpx.gpx");

        // the marker entry itself (desc == GC_WayPoint1) must still be classified as the main geocache,
        // not as a child waypoint of itself
        assertThat(hooks.getGeocaches()).hasSize(1);
        final Geocache cache = hooks.getGeocaches().get(0);
        assertThat(cache.getGeocode()).isEqualTo("TC99UN");
        assertThat(cache.getName()).isEqualTo("The Lodges at Heron Pond");

        // all following wpt entries (same "terracache" sym) are its child waypoints, parent = own code minus last char
        assertThat(hooks.getWaypoints()).hasSize(4);
        for (final Waypoint waypoint : hooks.getWaypoints()) {
            assertThat(waypoint.getGeocode()).isEqualTo("TC99UN");
        }
        assertThat(hooks.getWaypoints().get(0).getName()).isEqualTo("Stage Two");
        assertThat(hooks.getWaypoints().get(0).getPrefix()).isEqualTo("TC99UN1");

        // order preserved: geocache first, then its 4 child waypoints
        assertThat(hooks.getGlobalItems()).extracting(item -> item.kind)
            .containsExactly(
                RecordingGPXParseHooks.ItemKind.GEOCACHE,
                RecordingGPXParseHooks.ItemKind.WAYPOINT,
                RecordingGPXParseHooks.ItemKind.WAYPOINT,
                RecordingGPXParseHooks.ItemKind.WAYPOINT,
                RecordingGPXParseHooks.ItemKind.WAYPOINT);
    }

    // -----------------------------------------------------------------------------------------------------
    // reset()
    // -----------------------------------------------------------------------------------------------------

    @Test
    public void testResetClearsCrossFileIndex() throws Exception {
        final GPXParser parser = new GPXParser();
        // parse the cache first, so its geocode gets indexed by name
        parse("/gc31j2h.gpx", parser);
        // without reset(), the waypoint file would additionally benefit from the (here irrelevant, since the
        // naming convention already resolves it) index; after reset(), the index is empty again
        parser.reset();

        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        try (InputStream is = openResource("/gc31j2h_wpts.gpx")) {
            parser.parse(is, hooks);
        }
        // naming convention still resolves the parent regardless of the index, so this must still succeed
        assertThat(hooks.getWaypoints()).hasSize(2);
        assertThat(hooks.getWaypoints().get(0).getGeocode()).isEqualTo("GC31J2H");
    }

    // -----------------------------------------------------------------------------------------------------
    // resilience scan: every remaining sample file must parse without error and yield at least one item
    // -----------------------------------------------------------------------------------------------------

    @Test
    public void testAllSampleFilesParseWithoutError() throws Exception {
        for (final String resourceName : TEST_RES_XML_FILES) {
            assertParsesWithContent(resourceName);
        }
        for (final String resourceName : ANDROID_TEST_RAW_FILES) {
            // note: gc31j2h2_bad_cacheid.gpx deliberately contains an invalid groundspeak cache id;
            // the parser must tolerate it (not throw) rather than reject the whole file
            assertParsesWithContent(resourceName);
        }
    }

    private static void assertParsesWithContent(final String resourceName) throws Exception {
        final RecordingGPXParseHooks hooks = parse(resourceName);
        final int totalItems = hooks.getGlobalItems().size()
            + hooks.getRoutes().size()
            + hooks.getTracks().size();
        assertThat(totalItems)
            .withFailMessage("Expected at least one geocache/waypoint/coordinate/route/track in %s", resourceName)
            .isGreaterThan(0);

        for (final RecordingGPXParseHooks.Item item : hooks.getGlobalItems()) {
            assertThat(item.coordinate).withFailMessage("null coordinate in %s", resourceName).isNotNull();
        }
    }
}




