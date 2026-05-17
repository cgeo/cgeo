package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.utils.xml.XmlUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Focused tests for {@link UnifiedGPXTrackParser#parseTrack(XmlPullParser)}.
 * The parser under test reads a single {@code <trk>} element; these tests
 * drive it from XML fragments wrapped in a minimal {@code <gpx>} envelope.
 */
public class UnifiedGPXTrackParserTest {

    /**
     * Build an XmlPullParser positioned on the {@code <trk>} START_TAG of the
     * supplied fragment. The fragment must contain exactly one trk element.
     */
    private static XmlPullParser parserAtTrk(final String trkFragment) throws Exception {
        final String xml = "<?xml version=\"1.0\"?>"
                + "<gpx version=\"1.1\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/1\">"
                + trkFragment
                + "</gpx>";
        final XmlPullParser parser = XmlUtils.createParser(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), true);
        // advance until we are sitting on the <trk> START_TAG
        while (!(parser.getEventType() == XmlPullParser.START_TAG && "trk".equals(parser.getName()))) {
            if (parser.getEventType() == XmlPullParser.END_DOCUMENT) {
                throw new IllegalStateException("no <trk> found in test fragment");
            }
            parser.next();
        }
        return parser;
    }

    @Test
    public void singleSegmentSinglePoint() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk><trkseg><trkpt lat=\"1\" lon=\"2\"/></trkseg></trk>"));

        assertThat(route.isRouteable()).isFalse();
        assertThat(route.getNumSegments()).isEqualTo(1);
        assertThat(route.getSegments()[0].getPoints()).hasSize(1);
        assertThat(route.getSegments()[0].getLinkToPreviousSegment()).isFalse();
    }

    @Test
    public void trackNameIsRead() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk><name>Black Forest Loop</name>"
                + "<trkseg><trkpt lat=\"1\" lon=\"2\"/></trkseg>"
                + "</trk>"));
        assertThat(route.getName()).isEqualTo("Black Forest Loop");
    }

    @Test
    public void multipleSegmentsBecomeMultipleRouteSegments() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk>"
                + "  <trkseg><trkpt lat=\"1\" lon=\"1\"/><trkpt lat=\"1.1\" lon=\"1.1\"/></trkseg>"
                + "  <trkseg><trkpt lat=\"2\" lon=\"2\"/><trkpt lat=\"2.1\" lon=\"2.1\"/><trkpt lat=\"2.2\" lon=\"2.2\"/></trkseg>"
                + "  <trkseg><trkpt lat=\"3\" lon=\"3\"/></trkseg>"
                + "</trk>"));

        assertThat(route.getNumSegments()).isEqualTo(3);
        assertThat(route.getSegments()[0].getPoints()).hasSize(2);
        assertThat(route.getSegments()[1].getPoints()).hasSize(3);
        assertThat(route.getSegments()[2].getPoints()).hasSize(1);
        assertThat(route.getNumPoints()).isEqualTo(6);
    }

    @Test
    public void everySegmentIsNotLinkedToPredecessor() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk>"
                + "<trkseg><trkpt lat=\"1\" lon=\"1\"/></trkseg>"
                + "<trkseg><trkpt lat=\"2\" lon=\"2\"/></trkseg>"
                + "</trk>"));
        for (RouteSegment seg : route.getSegments()) {
            assertThat(seg.getLinkToPreviousSegment()).isFalse();
        }
    }

    @Test
    public void elevationKeptWhenEveryTrkptHasEle() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk><trkseg>"
                + "<trkpt lat=\"1\" lon=\"1\"><ele>100.0</ele></trkpt>"
                + "<trkpt lat=\"2\" lon=\"2\"><ele>200.5</ele></trkpt>"
                + "<trkpt lat=\"3\" lon=\"3\"><ele>300</ele></trkpt>"
                + "</trkseg></trk>"));

        final ArrayList<Float> elevations = route.getSegments()[0].getElevation();
        assertThat(elevations).isNotNull();
        assertThat(elevations).hasSize(3);
        assertThat(elevations.get(0)).isEqualTo(100.0f, offset(1e-6f));
        assertThat(elevations.get(1)).isEqualTo(200.5f, offset(1e-6f));
        assertThat(elevations.get(2)).isEqualTo(300.0f, offset(1e-6f));
    }

    @Test
    public void elevationDroppedWhenSomeTrkptsLackEle() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk><trkseg>"
                + "<trkpt lat=\"1\" lon=\"1\"><ele>100</ele></trkpt>"
                + "<trkpt lat=\"2\" lon=\"2\"/>"
                + "<trkpt lat=\"3\" lon=\"3\"><ele>300</ele></trkpt>"
                + "</trkseg></trk>"));

        // partial elevation data → list dropped so the elevation service can re-fetch
        assertThat(route.getSegments()[0].getElevation()).isNull();
        assertThat(route.getSegments()[0].getPoints()).hasSize(3);
    }

    @Test
    public void elevationDroppedWhenNoTrkptHasEle() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk><trkseg>"
                + "<trkpt lat=\"1\" lon=\"1\"/>"
                + "<trkpt lat=\"2\" lon=\"2\"/>"
                + "</trkseg></trk>"));
        assertThat(route.getSegments()[0].getElevation()).isNull();
    }

    @Test
    public void unparseableElevationTreatedAsMissing() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk><trkseg>"
                + "<trkpt lat=\"1\" lon=\"1\"><ele>100</ele></trkpt>"
                + "<trkpt lat=\"2\" lon=\"2\"><ele>not-a-number</ele></trkpt>"
                + "</trkseg></trk>"));
        // one good ele + one unparseable → not aligned → list dropped
        assertThat(route.getSegments()[0].getElevation()).isNull();
        assertThat(route.getSegments()[0].getPoints()).hasSize(2);
    }

    @Test
    public void trkptWithUnparseableCoordsIsDropped() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk><trkseg>"
                + "<trkpt lat=\"1\" lon=\"1\"/>"
                + "<trkpt lat=\"abc\" lon=\"2\"/>"
                + "<trkpt lat=\"3\" lon=\"3\"/>"
                + "</trkseg></trk>"));
        assertThat(route.getSegments()[0].getPoints()).hasSize(2);
    }

    @Test
    public void emptyTrksegProducesNoSegment() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk>"
                + "<trkseg></trkseg>"
                + "<trkseg><trkpt lat=\"1\" lon=\"1\"/></trkseg>"
                + "</trk>"));
        // empty trkseg is skipped, only the segment with a real point survives
        assertThat(route.getNumSegments()).isEqualTo(1);
    }

    @Test
    public void unknownChildrenInsideTrkAreSkipped() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk>"
                + "  <desc>track description, currently ignored</desc>"
                + "  <extensions><gpxx:Color xmlns:gpxx=\"urn:x\">Red</gpxx:Color></extensions>"
                + "  <trkseg><trkpt lat=\"1\" lon=\"1\"/></trkseg>"
                + "</trk>"));
        assertThat(route.getNumSegments()).isEqualTo(1);
    }

    @Test
    public void coordinatesAreRoundTripped() throws Exception {
        final Route route = UnifiedGPXTrackParser.parseTrack(parserAtTrk(
                "<trk><trkseg>"
                + "<trkpt lat=\"48.123456\" lon=\"9.654321\"/>"
                + "</trkseg></trk>"));
        final Geopoint p = route.getSegments()[0].getPoints().get(0);
        assertThat(p.getLatitude()).isEqualTo(48.123456, offset(1e-9));
        assertThat(p.getLongitude()).isEqualTo(9.654321, offset(1e-9));
    }
}
