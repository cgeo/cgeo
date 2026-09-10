package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.NamedGeoCoordinate;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.gpx.GPXParser.ParseMode;
import cgeo.geocaching.utils.xml.XmlUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GPXParserLifecycleTest {

    @Test
    public void testSwitchToFullRetainsMetadataAndPropagatesHookModes() throws Exception {
        final GPXParser parser = new GPXParser().setParseMode(ParseMode.COORDINATES_ONLY);
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks() {
            @Override
            public ParseMode onNamedCoordinate(final NamedGeoCoordinate coordinate) {
                super.onNamedCoordinate(coordinate);
                return ParseMode.FULL;
            }

            @Override
            public ParseMode onGeocache(final Geocache geocache, final List<LogEntry> logs) {
                super.onGeocache(geocache, logs);
                return ParseMode.COORDINATES_ONLY;
            }

            @Override
            public ParseMode onWaypoint(final Waypoint waypoint, final String parentGeocode) {
                super.onWaypoint(waypoint, parentGeocode);
                return ParseMode.ABORT;
            }
        };
        final String xml = GPXParserCompatibilityTest.document("<metadata><link href=\"https://extremcaching.com\"/></metadata>"
                + "<wpt><name>Enable full parsing</name></wpt>"
                + "<wpt><name>GCEC12345</name><sym>Geocache</sym></wpt>"
                + "<wpt><name>GCEC23456</name><sym>Geocache</sym></wpt>"
                + "<wpt><name>00EC12345</name><type>Waypoint|Reference Point</type></wpt>"
                + "<wpt><name>Must not be parsed</name></wpt>");
        try (InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            assertThat(parser.parse(XmlUtils.createParser(stream, true), hooks)).isFalse();
        }
        assertThat(hooks.getGlobalItems()).extracting(item -> item.kind).containsExactly(
                RecordingGPXParseHooks.ItemKind.NAMED_COORDINATE,
                RecordingGPXParseHooks.ItemKind.GEOCACHE,
                RecordingGPXParseHooks.ItemKind.NAMED_COORDINATE,
                RecordingGPXParseHooks.ItemKind.WAYPOINT);
        assertThat(hooks.getGeocaches().get(0).getGeocode()).isEqualTo("EC12345");
        assertThat(hooks.getWaypoints().get(0).getGeocode()).isEqualTo("EC12345");
    }

    @Test
    public void testTerraCachingStateClearedBetweenDocuments() throws Exception {
        final GPXParser parser = new GPXParser();
        final String marker = "<wpt><name>TC12345</name><sym>Terracache</sym><desc>GC_WayPoint1</desc></wpt>";
        final String child = "<wpt><name>TC123451</name><sym>Terracache</sym></wpt>";
        final RecordingGPXParseHooks first = GPXParserCompatibilityTest.parse(GPXParserCompatibilityTest.document(marker + child), parser);
        assertThat(first.getGeocaches()).hasSize(1);
        assertThat(first.getWaypoints()).hasSize(1);

        parser.setParseMode(ParseMode.SKIP);
        GPXParserCompatibilityTest.parse(GPXParserCompatibilityTest.document(marker), parser);
        parser.setParseMode(ParseMode.FULL);
        final RecordingGPXParseHooks next = GPXParserCompatibilityTest.parse(GPXParserCompatibilityTest.document(child), parser);
        assertThat(next.getGeocaches()).hasSize(1);
        assertThat(next.getWaypoints()).isEmpty();
    }
}
