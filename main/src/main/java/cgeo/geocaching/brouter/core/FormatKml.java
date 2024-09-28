package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.mapaccess.MatchedWaypoint;
import cgeo.geocaching.brouter.util.StringUtils;

import java.util.List;

public class FormatKml extends Formatter {
    public FormatKml(RoutingContext rc) {
        super(rc);
    }

    @Override
    public String format(OsmTrack t) {
        final StringBuilder sb = new StringBuilder(8192);

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        sb.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">\n");
        sb.append("  <Document>\n");
        sb.append("    <name>KML Samples</name>\n");
        sb.append("    <open>1</open>\n");
        sb.append("    <distance>3.497064</distance>\n");
        sb.append("    <traveltime>872</traveltime>\n");
        sb.append("    <description>To enable simple instructions add: 'instructions=1' as parameter to the URL</description>\n");
        sb.append("    <Folder>\n");
        sb.append("      <name>Paths</name>\n");
        sb.append("      <visibility>0</visibility>\n");
        sb.append("      <description>Examples of paths.</description>\n");
        sb.append("      <Placemark>\n");
        sb.append("        <name>Tessellated</name>\n");
        sb.append("        <visibility>0</visibility>\n");
        sb.append("        <description><![CDATA[If the <tessellate> tag has a value of 1, the line will contour to the underlying terrain]]></description>\n");
        sb.append("        <LineString>\n");
        sb.append("          <tessellate>1</tessellate>\n");
        sb.append("         <coordinates>");

        for (OsmPathElement n : t.nodes) {
            sb.append(formatILon(n.getILon())).append(",").append(formatILat(n.getILat())).append("\n");
        }

        sb.append("          </coordinates>\n");
        sb.append("        </LineString>\n");
        sb.append("      </Placemark>\n");
        sb.append("    </Folder>\n");
        if (t.exportWaypoints || !t.pois.isEmpty()) {
            if (!t.pois.isEmpty()) {
                sb.append("    <Folder>\n");
                sb.append("      <name>poi</name>\n");
                for (int i = 0; i < t.pois.size(); i++) {
                    final OsmNodeNamed poi = t.pois.get(i);
                    createPlaceMark(sb, poi.name, poi.ilat, poi.ilon);
                }
                sb.append("    </Folder>\n");
            }

            if (t.exportWaypoints) {
                final int size = t.matchedWaypoints.size();
                createFolder(sb, "start", t.matchedWaypoints.subList(0, 1));
                if (t.matchedWaypoints.size() > 2) {
                    createFolder(sb, "via", t.matchedWaypoints.subList(1, size - 1));
                }
                createFolder(sb, "end", t.matchedWaypoints.subList(size - 1, size));
            }
        }
        sb.append("  </Document>\n");
        sb.append("</kml>\n");

        return sb.toString();
    }

    private void createFolder(StringBuilder sb, String type, List<MatchedWaypoint> waypoints) {
        sb.append("    <Folder>\n");
        sb.append("      <name>").append(type).append("</name>\n");
        for (int i = 0; i < waypoints.size(); i++) {
            final MatchedWaypoint wp = waypoints.get(i);
            createPlaceMark(sb, wp.name, wp.waypoint.ilat, wp.waypoint.ilon);
        }
        sb.append("    </Folder>\n");
    }

    private void createPlaceMark(StringBuilder sb, String name, int ilat, int ilon) {
        sb.append("      <Placemark>\n");
        sb.append("        <name>").append(StringUtils.escapeXml10(name)).append("</name>\n");
        sb.append("        <Point>\n");
        sb.append("         <coordinates>").append(formatILon(ilon)).append(",").append(formatILat(ilat)).append("</coordinates>\n");
        sb.append("        </Point>\n");
        sb.append("      </Placemark>\n");
    }

}
