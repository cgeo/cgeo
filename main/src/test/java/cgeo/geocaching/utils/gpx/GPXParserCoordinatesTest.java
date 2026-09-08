package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.ICoordinate;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GPXParserCoordinatesTest {

    private static final String[] POINT_TAGS = {"wpt", "rtept", "trkpt"};
    private static final String[] FIELDS = {"", "<name>Named point</name><ele>123</ele>",
        "<name>GC12345</name><sym>Geocache</sym>", "<name>AA12345</name><type>Waypoint|Reference Point</type>"};
    private static final RecordingGPXParseHooks.ItemKind[] KINDS = {RecordingGPXParseHooks.ItemKind.COORDINATE,
        RecordingGPXParseHooks.ItemKind.NAMED_COORDINATE, RecordingGPXParseHooks.ItemKind.GEOCACHE, RecordingGPXParseHooks.ItemKind.WAYPOINT};

    private static String pointDocument(final String tag, final String attributes, final String fields) {
        final String point = "<" + tag + " " + attributes + ">" + fields + "</" + tag + ">";
        final String body;
        if ("rtept".equals(tag)) {
            body = "<rte>" + point + "</rte>";
        } else if ("trkpt".equals(tag)) {
            body = "<trk><trkseg>" + point + "</trkseg></trk>";
        } else {
            body = point;
        }
        return GPXParserCompatibilityTest.document(body);
    }

    private static GPXParser parser(final GPXParser.ParseMode mode) {
        return new GPXParser().setParseMode(mode);
    }

    private static void assertCounts(final RecordingGPXParseHooks hooks, final String tag) {
        if ("rtept".equals(tag)) {
            assertThat(hooks.getRoutes().get(0).pointCount).isEqualTo(1);
        } else if ("trkpt".equals(tag)) {
            assertThat(hooks.getTracks().get(0).totalPointCount).isEqualTo(1);
            assertThat(hooks.getTracks().get(0).segments.get(0).pointCount).isEqualTo(1);
        }
    }

    private static void assertCoordinates(final String attributes, final Geopoint genericCoords, final Geopoint cacheCoords) throws Exception {
        for (final String tag : POINT_TAGS) {
            for (int kind = 0; kind < FIELDS.length; kind++) {
                for (final GPXParser.ParseMode mode : new GPXParser.ParseMode[] {GPXParser.ParseMode.FULL, GPXParser.ParseMode.COORDINATES_ONLY}) {
                    final RecordingGPXParseHooks hooks = GPXParserCompatibilityTest.parse(pointDocument(tag, attributes, FIELDS[kind]), parser(mode));
                    assertThat(hooks.getGlobalItems()).as("%s %s %s %s", tag, kind, mode, attributes).hasSize(1);
                    final RecordingGPXParseHooks.Item item = hooks.getGlobalItems().get(0);
                    final boolean fullEntity = mode == GPXParser.ParseMode.FULL && kind >= 2;
                    assertThat(item.coordinate.getCoords()).isEqualTo(fullEntity ? cacheCoords : genericCoords);
                    assertThat(item.kind).isEqualTo(mode == GPXParser.ParseMode.FULL ? KINDS[kind]
                            : kind == 0 ? RecordingGPXParseHooks.ItemKind.COORDINATE : RecordingGPXParseHooks.ItemKind.NAMED_COORDINATE);
                    if (kind == 1) {
                        assertThat(item.coordinate.getElevation()).isEqualTo(123f);
                    }
                    assertCounts(hooks, tag);
                }
            }
        }
    }

    @Test
    public void testMissingAndPartialCoordinatesAcrossAllPointTypesAndModes() throws Exception {
        assertCoordinates("", new Geopoint(0, 0), null);
        assertCoordinates("lat=\"48\"", new Geopoint(48, 0), null);
        assertCoordinates("lon=\"9\"", new Geopoint(0, 9), null);
        assertCoordinates("lat=\" \" lon=\"\"", new Geopoint(0, 0), null);
    }

    @Test
    public void testInvalidCoordinatesAreReplacedIndependently() throws Exception {
        for (final String invalid : new String[] {"invalid", "NaN", "Infinity", "-Infinity", "1e999", "181", "-181"}) {
            assertCoordinates("lat=\"" + invalid + "\" lon=\"9\"", new Geopoint(0, 9), null);
            assertCoordinates("lat=\"48\" lon=\"" + invalid + "\"", new Geopoint(48, 0), null);
        }
        assertCoordinates("lat=\"91\" lon=\"-180\"", new Geopoint(0, -180), null);
        assertCoordinates("lat=\"-91\" lon=\"180\"", new Geopoint(0, 180), null);
    }

    @Test
    public void testValidZeroAndBoundaryCoordinates() throws Exception {
        assertCoordinates("lat=\"0\" lon=\"0\"", new Geopoint(0, 0), null);
        assertCoordinates("lat=\"0\" lon=\"9\"", new Geopoint(0, 9), new Geopoint(0, 9));
        assertCoordinates("lat=\"48\" lon=\"0\"", new Geopoint(48, 0), new Geopoint(48, 0));
        assertCoordinates("lat=\"90\" lon=\"180\"", new Geopoint(90, 180), new Geopoint(90, 180));
        assertCoordinates("lat=\"-90\" lon=\"-180\"", new Geopoint(-90, -180), new Geopoint(-90, -180));
    }

    @Test
    public void testSkipStillCountsEveryRouteAndTrackPoint() throws Exception {
        for (final String tag : POINT_TAGS) {
            for (int kind = 0; kind < FIELDS.length; kind++) {
                final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
                hooks.setModes(GPXParser.ParseMode.COORDINATES_ONLY, GPXParser.ParseMode.SKIP, GPXParser.ParseMode.SKIP);
                final String document = pointDocument(tag, "", FIELDS[kind]);
                GPXParserCompatibilityTest.parse(document, parser(GPXParser.ParseMode.SKIP), hooks);
                // Cache/waypoint SKIP modes doesn't suppress generic top-level coordinates.
                assertThat(hooks.getGlobalItems()).hasSize("wpt".equals(tag) ? 1 : 0);
                assertCounts(hooks, tag);
            }
        }
    }

    @Test
    public void testMixedEntriesStayInDocumentOrder() throws Exception {
        final String body = "<wpt><name>GC12345</name><sym>Geocache</sym></wpt><wpt lon=\"9\"/>"
                + "<wpt><name>AA12345</name><type>Waypoint|Reference Point</type></wpt><wpt lat=\"48\"><name>Last</name></wpt>";
        final RecordingGPXParseHooks hooks = GPXParserCompatibilityTest.parse(GPXParserCompatibilityTest.document(body), new GPXParser());
        assertThat(hooks.getGlobalItems()).extracting(i -> i.kind).containsExactly(
                RecordingGPXParseHooks.ItemKind.GEOCACHE, RecordingGPXParseHooks.ItemKind.COORDINATE,
                RecordingGPXParseHooks.ItemKind.WAYPOINT, RecordingGPXParseHooks.ItemKind.NAMED_COORDINATE);
        assertThat(hooks.getGlobalItems()).extracting(i -> i.coordinate).extracting(ICoordinate::getCoords)
                .containsExactly(null, new Geopoint(0, 9), null, new Geopoint(48, 0));
    }
}
