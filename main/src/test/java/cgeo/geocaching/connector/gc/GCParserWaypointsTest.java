package cgeo.geocaching.connector.gc;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GCParserWaypointsTest {

    /** waypoint table of a cache listing, reduced to the parts parseWaypoints looks at */
    private static String waypointTable(final String nameHtml) {
        return "<table id=\"ctl00_ContentBody_Waypoints\"><tbody>"
                + "<tr ishidden=\"false\"><td></td>"
                + "<td><img src=\"/images/WptTypes/sm/pkg.jpg\" alt=\"Parking Area\" /></td>"
                + "<td><span id=\"awpt_01\">01</span></td>"
                + "<td>01</td>"
                + "<td><a href=\"/seek/wpt.aspx?WID=1\">" + nameHtml + "</a> (Parking Area)</td>"
                + "<td>N 48° 00.000 E 011° 00.000</td>"
                + "<td></td></tr>"
                + "<tr><td></td><td>Note:</td><td colspan=\"6\">some note</td></tr>"
                + "</tbody></table>";
    }

    private static String parseName(final String nameHtml) {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC1234");
        GCParser.parseWaypoints(cache, waypointTable(nameHtml));
        final List<Waypoint> waypoints = cache.getWaypoints();
        assertThat(waypoints).hasSize(1);
        return waypoints.get(0).getName();
    }

    @Test
    public void nameWithQuotationMarks() {
        assertThat(parseName("&quot;Old&quot; parking")).isEqualTo("\"Old\" parking");
    }

    @Test
    public void nameWithOtherEntities() {
        assertThat(parseName("Bob&#39;s bench &amp; table")).isEqualTo("Bob's bench & table");
    }

    @Test
    public void plainName() {
        assertThat(parseName("Parking")).isEqualTo("Parking");
    }
}
