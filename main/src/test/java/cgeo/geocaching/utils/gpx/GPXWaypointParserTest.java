package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.WaypointUserNoteCombiner;
import cgeo.geocaching.utils.xml.XmlUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GPXWaypointParserTest {

    private static String document(final boolean gpx11, final String body) {
        return "<gpx version=\"" + (gpx11 ? "1.1" : "1.0") + "\" xmlns=\"http://www.topografix.com/GPX/1/" + (gpx11 ? "1" : "0")
                + "\" xmlns:cgeo=\"http://www.cgeo.org/wptext/1/0\" xmlns:gsak=\"http://www.gsak.net/xmlv1/6\">" + body + "</gpx>";
    }

    private static String waypoint(final boolean gpx11, final String attributes, final String name, final String note, final String extensions) {
        return "<wpt " + attributes + "><name>" + name + "</name><desc>My stage</desc><cmt>" + note
                + "</cmt><sym>Reference Point</sym><type>Waypoint|Reference Point</type>"
                + (gpx11 ? "<extensions>" + extensions + "</extensions>" : extensions) + "</wpt>";
    }

    private static RecordingGPXParseHooks parse(final String xml, final GPXParser parser, final boolean namespaceAware) throws Exception {
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        try (InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            parser.parse(XmlUtils.createParser(stream, namespaceAware), hooks);
        }
        return hooks;
    }

    @Test
    public void testCgeoWaypointExtensionsInBothGpxVersionsAndNamespaceModes() throws Exception {
        for (final boolean gpx11 : new boolean[] {false, true}) {
            for (final boolean namespaceAware : new boolean[] {false, true}) {
                final String extensions = "<gsak:wptExtension><gsak:Parent>GC12345</gsak:Parent></gsak:wptExtension>"
                        + "<cgeo:visited> true </cgeo:visited><cgeo:userdefined> true </cgeo:userdefined>"
                        + "<cgeo:originalCoordsEmpty> true </cgeo:originalCoordsEmpty>";
                final String xml = document(gpx11, waypoint(gpx11, "lat=\"48\" lon=\"9\"", "OWN-AA12345", "My personal note", extensions)
                        + waypoint(gpx11, "lat=\"48\" lon=\"9\"", "BB12345", "Listing note", ""));
                final RecordingGPXParseHooks hooks = parse(xml, new GPXParser(), namespaceAware);
                assertThat(hooks.getWaypoints()).hasSize(2);
                final Waypoint own = hooks.getWaypoints().get(0);
                assertThat(own.getName()).isEqualTo("My stage");
                assertThat(own.getPrefix()).isEqualTo("AA");
                assertThat(own.getGeocode()).isEqualTo("GC12345");
                assertThat(own.getLookup()).isEqualTo("---");
                assertThat(own.getWaypointType()).isEqualTo(WaypointType.WAYPOINT);
                assertThat(own.isUserDefined()).isTrue();
                assertThat(own.isVisited()).isTrue();
                assertThat(own.isOriginalCoordsEmpty()).isTrue();
                assertThat(own.getCoords()).isNotNull();
                assertThat(own.getUserNote()).isEqualTo("My personal note");
                assertThat(own.getNote()).isEmpty();
                final Waypoint listing = hooks.getWaypoints().get(1);
                assertThat(listing.isUserDefined()).isFalse();
                assertThat(listing.isVisited()).isFalse();
                assertThat(listing.isOriginalCoordsEmpty()).isFalse();
                assertThat(listing.getPrefix()).isEqualTo("BB");
                assertThat(listing.getNote()).isEqualTo("Listing note");
                assertThat(listing.getUserNote()).isEmpty();
            }
        }
    }

    @Test
    public void testGsakUserDefinedWaypoints() throws Exception {
        for (final boolean gpx11 : new boolean[] {false, true}) {
            final String extensions = "<gsak:wptExtension><gsak:Parent>OCDDD2</gsak:Parent>"
                    + "<gsak:Child_ByGSAK> true </gsak:Child_ByGSAK></gsak:wptExtension>";
            final String xml = document(gpx11, waypoint(gpx11, "lat=\"48\" lon=\"9\"", "OWN-021", "A user note", extensions));
            final Waypoint wp = parse(xml, new GPXParser(), false).getWaypoints().get(0);
            assertThat(wp.isUserDefined()).isTrue();
            assertThat(wp.getGeocode()).isEqualTo("OCDDD2");
            assertThat(wp.getPrefix()).isEqualTo("021");
            assertThat(wp.getUserNote()).isEqualTo("A user note");
            assertThat(wp.getNote()).isEmpty();
        }
    }

    @Test
    public void testFalseExtensionFlags() throws Exception {
        final String extensions = "<gsak:wptExtension><gsak:Child_ByGSAK>false</gsak:Child_ByGSAK></gsak:wptExtension>"
                + "<cgeo:visited>false</cgeo:visited><cgeo:userdefined>false</cgeo:userdefined>"
                + "<cgeo:originalCoordsEmpty>false</cgeo:originalCoordsEmpty>";
        final String xml = document(true, waypoint(true, "lat=\"48\" lon=\"9\"", "AA12345", "", extensions));
        final Waypoint wp = parse(xml, new GPXParser(), false).getWaypoints().get(0);
        assertThat(wp.isUserDefined()).isFalse();
        assertThat(wp.isVisited()).isFalse();
        assertThat(wp.isOriginalCoordsEmpty()).isFalse();
    }

    @Test
    public void testCombinedNotesRoundTrip() throws Exception {
        for (final boolean own : new boolean[] {false, true}) {
            for (final String listingNote : new String[] {"", "Listing note"}) {
                final Waypoint source = new Waypoint("My stage", WaypointType.WAYPOINT, own);
                source.setNote(listingNote);
                source.setUserNote("My personal note\n--\nwith another separator");
                final String note = new WaypointUserNoteCombiner(source).getCombinedNoteAndUserNote();
                final String xml = document(true, waypoint(true, "lat=\"48\" lon=\"9\"", "AA12345", note,
                        "<cgeo:userdefined>" + own + "</cgeo:userdefined>"));
                final Waypoint parsed = parse(xml, new GPXParser(), false).getWaypoints().get(0);
                assertThat(parsed.getNote()).isEqualTo(own ? "" : listingNote);
                assertThat(parsed.getUserNote()).isEqualTo(source.getUserNote());
            }
        }
    }

    @Test
    public void testEmptyCoordinatesForListingAndUserDefinedWaypoints() throws Exception {
        for (final String attributes : new String[] {"lat=\"0\" lon=\"0\"", "lat=\"\" lon=\"\"", "", "lat=\"invalid\" lon=\"9\""}) {
            for (final boolean own : new boolean[] {false, true}) {
                final String xml = document(true, waypoint(true, attributes, "AA12345", "",
                        "<cgeo:userdefined>" + own + "</cgeo:userdefined>"));
                final RecordingGPXParseHooks hooks = parse(xml, new GPXParser(), false);
                assertThat(hooks.getWaypoints()).hasSize(1);
                final Waypoint wp = hooks.getWaypoints().get(0);
                assertThat(wp.getCoords()).isNull();
                assertThat(wp.isUserDefined()).isEqualTo(own);
                assertThat(wp.isOriginalCoordsEmpty()).isEqualTo(!own);
                assertThat(wp.getName()).isEqualTo("My stage");
            }
        }
    }

    @Test
    public void testWaypointsWithoutCoordinatesRespectParseModes() throws Exception {
        final String xml = document(true, waypoint(true, "lat=\"0\" lon=\"0\"", "AA12345", "", "")
                + waypoint(true, "lat=\"48\" lon=\"9\"", "BB12345", "", ""));
        final RecordingGPXParseHooks skipped = parse(xml, new GPXParser().setParseMode(ParseMode.SKIP), false);
        assertThat(skipped.getGlobalItems()).isEmpty();
        final RecordingGPXParseHooks coordinates = parse(xml, new GPXParser().setParseMode(ParseMode.COORDINATES_ONLY), false);
        assertThat(coordinates.getWaypoints()).isEmpty();
        assertThat(coordinates.getGlobalItems()).hasSize(2);
        assertThat(coordinates.getGlobalItems().get(0).coordinate.getCoords()).isNotNull();
    }

    @Test
    public void testTrackAndRouteCountsIncludeWaypointsWithoutCoordinates() throws Exception {
        final String points = waypoint(true, "lat=\"0\" lon=\"0\"", "AA12345", "", "")
                + waypoint(true, "lat=\"48\" lon=\"9\"", "BB12345", "", "");
        final String xml = document(true, "<rte>" + points.replace("wpt", "rtept") + "</rte><trk><trkseg>"
                + points.replace("wpt", "trkpt") + "</trkseg></trk>");
        final RecordingGPXParseHooks hooks = parse(xml, new GPXParser(), false);
        assertThat(hooks.getWaypoints()).hasSize(4);
        assertThat(hooks.getRoutes().get(0).pointCount).isEqualTo(2);
        assertThat(hooks.getTracks().get(0).totalPointCount).isEqualTo(2);
        assertThat(hooks.getTracks().get(0).segments.get(0).pointCount).isEqualTo(2);
    }

    @Test
    public void testWaypointWithoutNameOrParent() throws Exception {
        final String xml = document(true, "<wpt lat=\"48\" lon=\"9\"><type>Waypoint|Reference Point</type>"
                + "<extensions><cgeo:visited>true</cgeo:visited></extensions></wpt>");
        final RecordingGPXParseHooks hooks = parse(xml, new GPXParser(), false);
        assertThat(hooks.getWaypoints()).hasSize(1);
        final Waypoint wp = hooks.getWaypoints().get(0);
        assertThat(wp.getName()).isEmpty();
        assertThat(wp.getPrefix()).isEmpty();
        assertThat(wp.isVisited()).isTrue();
        assertThat(hooks.getGlobalItems().get(0).parentGeocode).isNull();
    }

    @Test
    public void testExportedOpencachingWaypointsWithEmptyCoordinates() throws Exception {
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        try (InputStream stream = GPXParserTest.openResource("/ocddd2_empty_coord.gpx")) {
            new GPXParser().parse(stream, hooks);
        }
        assertThat(hooks.getWaypoints()).hasSize(8);
        final Waypoint emptyOwn = hooks.getWaypoints().get(4);
        assertThat(emptyOwn.getName()).isEqualTo("Original Empty User");
        assertThat(emptyOwn.getCoords()).isNull();
        assertThat(emptyOwn.isUserDefined()).isTrue();
        assertThat(emptyOwn.isOriginalCoordsEmpty()).isFalse();
        final Waypoint modified = hooks.getWaypoints().get(6);
        assertThat(modified.getName()).isEqualTo("Original Empty Modified");
        assertThat(modified.getCoords()).isNotNull();
        assertThat(modified.isUserDefined()).isFalse();
        assertThat(modified.isOriginalCoordsEmpty()).isTrue();
        final Waypoint blank = hooks.getWaypoints().get(7);
        assertThat(blank.getName()).isEqualTo("Original Blank");
        assertThat(blank.getCoords()).isNull();
        assertThat(blank.isOriginalCoordsEmpty()).isTrue();
    }

    @Test
    public void testOriginalCoordinatesWaypoint() throws Exception {
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        try (InputStream stream = GPXParserTest.openResource("/gc31j2h_wpts_original.gpx")) {
            new GPXParser().parse(stream, hooks);
        }
        final Waypoint original = hooks.getWaypoints().get(0);
        assertThat(original.getWaypointType()).isEqualTo(WaypointType.ORIGINAL);
        assertThat(original.getName()).isEqualTo("Original Coordinates");
        assertThat(original.getPrefix()).isEqualTo("00");
        assertThat(original.getGeocode()).isEqualTo("GC31J2H");
    }
}
