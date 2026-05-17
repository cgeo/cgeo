package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteSegment;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.Test;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class UnifiedGPXParserTest {

    private static final String GPX_HEADER_11 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<gpx version=\"1.1\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/1\">";
    private static final String GPX_HEADER_10 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<gpx version=\"1.0\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/0\">";
    private static final String GPX_HEADER_NO_NS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<gpx version=\"1.1\" creator=\"test\">";
    private static final String GPX_FOOTER = "</gpx>";

    private static UnifiedGPXParser.Result parse(final String xml) throws Exception {
        return UnifiedGPXParser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void emptyGpx() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11 + GPX_FOOTER);
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.waypoints).isEmpty();
        assertThat(result.routes).isEmpty();
        assertThat(result.tracks).isEmpty();
    }

    @Test
    public void nonGpxRoot() throws Exception {
        final UnifiedGPXParser.Result result = parse(
                "<?xml version=\"1.0\"?><foo><bar lat=\"1\" lon=\"2\"/></foo>");
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void singleWaypointPopulatesAllSimpleFields() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<wpt lat=\"48.5\" lon=\"9.25\">"
                + "  <name>WP-Test</name>"
                + "  <desc>short text</desc>"
                + "  <cmt>long text</cmt>"
                + "  <time>2024-01-15T10:30:00Z</time>"
                + "</wpt>"
                + GPX_FOOTER);

        assertThat(result.waypoints).hasSize(1);
        final Geocache cache = result.waypoints.iterator().next();
        assertThat(cache.getCoords().getLatitude()).isEqualTo(48.5, offset(1e-9));
        assertThat(cache.getCoords().getLongitude()).isEqualTo(9.25, offset(1e-9));
        assertThat(cache.getName()).isEqualTo("WP-Test");
        // setGeocode uppercases the input
        assertThat(cache.getGeocode()).isEqualTo("WP-TEST");
        assertThat(cache.getShortDescription()).isEqualTo("short text");
        assertThat(cache.getDescription()).isEqualTo("long text");
        assertThat(cache.getHiddenDate()).isNotNull();
    }

    @Test
    public void waypointWithoutCoordsIsDropped() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<wpt><name>no coords</name></wpt>"
                + "<wpt lat=\"1\" lon=\"2\"><name>ok</name></wpt>"
                + GPX_FOOTER);
        assertThat(result.waypoints).hasSize(1);
    }

    @Test
    public void waypointWithUnparseableCoordsIsDropped() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<wpt lat=\"abc\" lon=\"2\"><name>bad</name></wpt>"
                + GPX_FOOTER);
        assertThat(result.waypoints).isEmpty();
    }

    @Test
    public void multipleWaypoints() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<wpt lat=\"1\" lon=\"1\"><name>A</name></wpt>"
                + "<wpt lat=\"2\" lon=\"2\"><name>B</name></wpt>"
                + "<wpt lat=\"3\" lon=\"3\"><name>C</name></wpt>"
                + GPX_FOOTER);
        assertThat(result.waypoints).hasSize(3);
    }

    @Test
    public void routeIsRoutableWithOneSegmentPerPoint() throws Exception {
        // No <name> on rtept on purpose: RouteItem(String, Geopoint) tries to resolve names
        // that look like geocodes via DataStore, which is not available in plain JVM tests.
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<rte>"
                + "  <name>My Route</name>"
                + "  <rtept lat=\"1\" lon=\"1\"/>"
                + "  <rtept lat=\"2\" lon=\"2\"/>"
                + "  <rtept lat=\"3\" lon=\"3\"/>"
                + "</rte>"
                + GPX_FOOTER);

        assertThat(result.routes).hasSize(1);
        final Route route = result.routes.get(0);
        assertThat(route.isRouteable()).isTrue();
        assertThat(route.getName()).isEqualTo("My Route");
        assertThat(route.getNumSegments()).isEqualTo(3);
        for (RouteSegment segment : route.getSegments()) {
            assertThat(segment.getPoints()).hasSize(1);
            assertThat(segment.getLinkToPreviousSegment()).isTrue();
        }
        // total point count
        assertThat(route.getNumPoints()).isEqualTo(3);
    }

    @Test
    public void trackIsNotRoutableAndProducesOneSegmentPerTrkseg() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<trk>"
                + "  <name>My Track</name>"
                + "  <trkseg>"
                + "    <trkpt lat=\"1\" lon=\"1\"/>"
                + "    <trkpt lat=\"1.1\" lon=\"1.1\"/>"
                + "  </trkseg>"
                + "  <trkseg>"
                + "    <trkpt lat=\"2\" lon=\"2\"/>"
                + "    <trkpt lat=\"2.1\" lon=\"2.1\"/>"
                + "    <trkpt lat=\"2.2\" lon=\"2.2\"/>"
                + "  </trkseg>"
                + "</trk>"
                + GPX_FOOTER);

        assertThat(result.tracks).hasSize(1);
        final Route track = result.tracks.get(0);
        assertThat(track.isRouteable()).isFalse();
        assertThat(track.getName()).isEqualTo("My Track");
        assertThat(track.getNumSegments()).isEqualTo(2);
        assertThat(track.getSegments()[0].getPoints()).hasSize(2);
        assertThat(track.getSegments()[1].getPoints()).hasSize(3);
        for (RouteSegment segment : track.getSegments()) {
            assertThat(segment.getLinkToPreviousSegment()).isFalse();
        }
    }

    @Test
    public void multipleTracksProduceMultipleRoutes() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<trk><name>A</name><trkseg><trkpt lat=\"1\" lon=\"1\"/></trkseg></trk>"
                + "<trk><name>B</name><trkseg><trkpt lat=\"2\" lon=\"2\"/></trkseg></trk>"
                + GPX_FOOTER);
        assertThat(result.tracks).hasSize(2);
        assertThat(result.tracks.get(0).getName()).isEqualTo("A");
        assertThat(result.tracks.get(1).getName()).isEqualTo("B");
    }

    @Test
    public void mixedContentFillsAllThreeLists() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<wpt lat=\"1\" lon=\"1\"><name>A</name></wpt>"
                + "<wpt lat=\"2\" lon=\"2\"><name>B</name></wpt>"
                + "<rte><rtept lat=\"3\" lon=\"3\"/></rte>"
                + "<trk><trkseg><trkpt lat=\"4\" lon=\"4\"/></trkseg></trk>"
                + GPX_FOOTER);

        assertThat(result.waypoints).hasSize(2);
        assertThat(result.routes).hasSize(1);
        assertThat(result.tracks).hasSize(1);
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    public void gpx10NamespaceIsAccepted() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_10
                + "<wpt lat=\"1\" lon=\"1\"><name>A</name></wpt>"
                + "<trk><trkseg><trkpt lat=\"2\" lon=\"2\"/></trkseg></trk>"
                + GPX_FOOTER);
        assertThat(result.waypoints).hasSize(1);
        assertThat(result.tracks).hasSize(1);
    }

    @Test
    public void noNamespaceIsAccepted() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_NO_NS
                + "<wpt lat=\"1\" lon=\"1\"><name>A</name></wpt>"
                + GPX_FOOTER);
        assertThat(result.waypoints).hasSize(1);
    }

    @Test
    public void unknownElementsAreSkipped() throws Exception {
        // metadata, extensions, and an unknown vendor element should be ignored
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<metadata><name>file</name><time>2024-01-15T10:30:00Z</time></metadata>"
                + "<wpt lat=\"1\" lon=\"1\">"
                + "  <name>A</name>"
                + "  <extensions><vendor:foo xmlns:vendor=\"urn:x\">bar</vendor:foo></extensions>"
                + "</wpt>"
                + "<unknown><stuff>ignored</stuff></unknown>"
                + GPX_FOOTER);

        assertThat(result.waypoints).hasSize(1);
        assertThat(result.routes).isEmpty();
        assertThat(result.tracks).isEmpty();
        assertThat(result.waypoints.iterator().next().getName()).isEqualTo("A");
    }

    @Test(expected = ParserException.class)
    public void invalidXmlThrowsParserException() throws Exception {
        parse(GPX_HEADER_11 + "<wpt lat=\"1\" lon=\"1\"><name>not closed");
    }

    @Test
    public void trkpointsArePreservedInOrder() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX_HEADER_11
                + "<trk><trkseg>"
                + "<trkpt lat=\"10\" lon=\"20\"/>"
                + "<trkpt lat=\"11\" lon=\"21\"/>"
                + "<trkpt lat=\"12\" lon=\"22\"/>"
                + "</trkseg></trk>"
                + GPX_FOOTER);

        final ArrayList<Geopoint> points = result.tracks.get(0).getSegments()[0].getPoints();
        assertThat(points).hasSize(3);
        assertThat(points.get(0).getLatitude()).isEqualTo(10.0, offset(1e-9));
        assertThat(points.get(0).getLongitude()).isEqualTo(20.0, offset(1e-9));
        assertThat(points.get(2).getLatitude()).isEqualTo(12.0, offset(1e-9));
        assertThat(points.get(2).getLongitude()).isEqualTo(22.0, offset(1e-9));
    }
}
