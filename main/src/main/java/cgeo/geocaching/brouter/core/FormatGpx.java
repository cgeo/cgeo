package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.brouter.mapaccess.MatchedWaypoint;
import cgeo.geocaching.brouter.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

public class FormatGpx extends Formatter {
    public FormatGpx(RoutingContext rc) {
        super(rc);
    }

    @Override
    public String format(OsmTrack t) {
        try {
            final StringWriter sw = new StringWriter(8192);
            final BufferedWriter bw = new BufferedWriter(sw);
            formatAsGpx(bw, t);
            bw.close();
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String formatAsGpx(final BufferedWriter sb, OsmTrack t) throws IOException {
        final int turnInstructionMode = t.voiceHints != null ? t.voiceHints.turnInstructionMode : 0;

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (turnInstructionMode != 9) {
            for (int i = t.messageList.size() - 1; i >= 0; i--) {
                String message = t.messageList.get(i);
                if (i < t.messageList.size() - 1) {
                    message = "(alt-index " + i + ": " + message + " )";
                }
                if (message != null) {
                    sb.append("<!-- ").append(message).append(" -->\n");
                }
            }
        }

        if (turnInstructionMode == 4) { // comment style
            sb.append("<!-- $transport-mode$").append(t.voiceHints.getTransportMode()).append("$ -->\n");
            sb.append("<!--          cmd    idx        lon        lat d2next  geometry -->\n");
            sb.append("<!-- $turn-instruction-start$\n");
            for (VoiceHint hint : t.voiceHints.list) {
                sb.append(String.format(Locale.getDefault(), "     $turn$%6s;%6d;%10s;%10s;%6d;%s$\n", hint.getCommandString(turnInstructionMode), hint.indexInTrack,
                        formatILon(hint.ilon), formatILat(hint.ilat), (int) (hint.distanceToNext), hint.formatGeometry()));
            }
            sb.append("    $turn-instruction-end$ -->\n");
        }
        sb.append("<gpx \n");
        sb.append(" xmlns=\"http://www.topografix.com/GPX/1/1\" \n");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
        if (turnInstructionMode == 9) { // BRouter style
            sb.append(" xmlns:brouter=\"Not yet documented\" \n");
        }
        if (turnInstructionMode == 7) { // old locus style
            sb.append(" xmlns:locus=\"http://www.locusmap.eu\" \n");
        }
        sb.append(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\" \n");

        if (turnInstructionMode == 3) {
            sb.append(" creator=\"OsmAndRouter\" version=\"1.1\">\n");
        } else {
            sb.append(" creator=\"BRouter-" + BRouterConstants.version + "\" version=\"1.1\">\n");
        }
        if (turnInstructionMode == 9) {
            sb.append(" <metadata>\n");
            sb.append("  <name>").append(t.name).append("</name>\n");
            sb.append("  <extensions>\n");
            sb.append("   <brouter:info>").append(t.messageList.get(0)).append("</brouter:info>\n");
            if (t.params != null && !t.params.isEmpty()) {
                sb.append("   <brouter:params><![CDATA[");
                int i = 0;
                for (Map.Entry<String, String> e : t.params.entrySet()) {
                    if (i++ != 0) {
                        sb.append("&");
                    }
                    sb.append(e.getKey()).append("=").append(e.getValue());
                }
                sb.append("]]></brouter:params>\n");
            }
            sb.append("  </extensions>\n");
            sb.append(" </metadata>\n");
        }
        if (turnInstructionMode == 3 || turnInstructionMode == 8) { // osmand style, cruiser
            float lastRteTime = 0;

            sb.append(" <rte>\n");

            float rteTime = t.getVoiceHintTime(0);
            final StringBuffer first = new StringBuffer();
            // define start point
            {
                first.append("  <rtept lat=\"").append(formatILat(t.nodes.get(0).getILat())).append("\" lon=\"")
                        .append(formatILon(t.nodes.get(0).getILon())).append("\">\n")
                        .append("   <desc>start</desc>\n   <extensions>\n");
                if (rteTime != lastRteTime) { // add timing only if available
                    final double ti = rteTime - lastRteTime;
                    first.append("    <time>").append("" + (int) (ti + 0.5)).append("</time>\n");
                    lastRteTime = rteTime;
                }
                first.append("    <offset>0</offset>\n  </extensions>\n </rtept>\n");
            }
            if (turnInstructionMode == 8) {
                if (t.matchedWaypoints.get(0).wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT && t.voiceHints.list.get(0).indexInTrack == 0) {
                    // has a voice hint do nothing, voice hint will do
                } else {
                    sb.append(first.toString());
                }
            } else {
                sb.append(first.toString());
            }

            for (int i = 0; i < t.voiceHints.list.size(); i++) {
                final VoiceHint hint = t.voiceHints.list.get(i);
                sb.append("  <rtept lat=\"").append(formatILat(hint.ilat)).append("\" lon=\"")
                        .append(formatILon(hint.ilon)).append("\">\n")
                        .append("   <desc>")
                        .append(turnInstructionMode == 3 ? hint.getMessageString(turnInstructionMode) : hint.getCruiserMessageString())
                        .append("</desc>\n   <extensions>\n");

                rteTime = t.getVoiceHintTime(i + 1);

                if (rteTime != lastRteTime) { // add timing only if available
                    final double ti = rteTime - lastRteTime;
                    sb.append("    <time>").append("" + (int) (ti + 0.5)).append("</time>\n");
                    lastRteTime = rteTime;
                }
                sb.append("    <turn>")
                        .append(turnInstructionMode == 3 ? hint.getCommandString(turnInstructionMode) : hint.getCruiserCommandString())
                        .append("</turn>\n    <turn-angle>").append("" + (int) hint.angle)
                        .append("</turn-angle>\n    <offset>").append("" + hint.indexInTrack).append("</offset>\n  </extensions>\n </rtept>\n");
            }
            sb.append("  <rtept lat=\"").append(formatILat(t.nodes.get(t.nodes.size() - 1).getILat())).append("\" lon=\"")
                    .append(formatILon(t.nodes.get(t.nodes.size() - 1).getILon())).append("\">\n")
                    .append("   <desc>destination</desc>\n   <extensions>\n");
            sb.append("    <time>0</time>\n");
            sb.append("    <offset>").append("" + (t.nodes.size() - 1)).append("</offset>\n  </extensions>\n </rtept>\n");

            sb.append("</rte>\n");
        }

        if (turnInstructionMode == 7) { // old locus style
            float lastRteTime = t.getVoiceHintTime(0);

            for (int i = 0; i < t.voiceHints.list.size(); i++) {
                final VoiceHint hint = t.voiceHints.list.get(i);
                sb.append(" <wpt lon=\"").append(formatILon(hint.ilon)).append("\" lat=\"")
                        .append(formatILat(hint.ilat)).append("\">")
                        .append(hint.selev == Short.MIN_VALUE ? "" : "<ele>" + (hint.selev / 4.) + "</ele>")
                        .append("<name>").append(hint.getMessageString(turnInstructionMode)).append("</name>")
                        .append("<extensions><locus:rteDistance>").append("" + hint.distanceToNext).append("</locus:rteDistance>");
                final float rteTime = t.getVoiceHintTime(i + 1);
                if (rteTime != lastRteTime) { // add timing only if available
                    final double ti = rteTime - lastRteTime;
                    final double speed = hint.distanceToNext / ti;
                    sb.append("<locus:rteTime>").append("" + ti).append("</locus:rteTime>")
                            .append("<locus:rteSpeed>").append("" + speed).append("</locus:rteSpeed>");
                    lastRteTime = rteTime;
                }
                sb.append("<locus:rtePointAction>").append("" + hint.getLocusAction()).append("</locus:rtePointAction></extensions>")
                        .append("</wpt>\n");
            }
        }
        if (turnInstructionMode == 5) { // gpsies style
            for (VoiceHint hint : t.voiceHints.list) {
                sb.append(" <wpt lon=\"").append(formatILon(hint.ilon)).append("\" lat=\"")
                        .append(formatILat(hint.ilat)).append("\">")
                        .append("<name>").append(hint.getMessageString(turnInstructionMode)).append("</name>")
                        .append("<sym>").append(hint.getSymbolString(turnInstructionMode).toLowerCase(Locale.ROOT)).append("</sym>")
                        .append("<type>").append(hint.getSymbolString(turnInstructionMode)).append("</type>")
                        .append("</wpt>\n");
            }
        }

        if (turnInstructionMode == 6) { // orux style
            for (VoiceHint hint : t.voiceHints.list) {
                sb.append(" <wpt lat=\"").append(formatILat(hint.ilat)).append("\" lon=\"")
                        .append(formatILon(hint.ilon)).append("\">")
                        .append(hint.selev == Short.MIN_VALUE ? "" : "<ele>" + (hint.selev / 4.) + "</ele>")
                        .append("<extensions>\n" +
                                "  <om:oruxmapsextensions xmlns:om=\"http://www.oruxmaps.com/oruxmapsextensions/1/0\">\n" +
                                "   <om:ext type=\"ICON\" subtype=\"0\">").append("" + hint.getOruxAction())
                        .append("</om:ext>\n" +
                                "  </om:oruxmapsextensions>\n" +
                                "  </extensions>\n" +
                                " </wpt>\n");
            }
        }

        for (int i = 0; i <= t.pois.size() - 1; i++) {
            final OsmNodeNamed poi = t.pois.get(i);
            formatWaypointGpx(sb, poi, "poi");
        }

        if (t.exportWaypoints) {
            for (int i = 0; i <= t.matchedWaypoints.size() - 1; i++) {
                final MatchedWaypoint wt = t.matchedWaypoints.get(i);
                if (i == 0) {
                    formatWaypointGpx(sb, wt, wt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT ? "beeline" : "via");
                } else if (i == t.matchedWaypoints.size() - 1) {
                    formatWaypointGpx(sb, wt, "via");
                } else {
                    if (wt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT) {
                        formatWaypointGpx(sb, wt, "beeline");
                    } else if (wt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_MEETING) {
                        formatWaypointGpx(sb, wt, "via");
                    } else {
                        formatWaypointGpx(sb, wt, "shaping");
                    }
                }
            }
        }
        if (t.exportCorrectedWaypoints) {
            sb.append("\n");
            for (int i = 0; i <= t.matchedWaypoints.size() - 1; i++) {
                final MatchedWaypoint wt = t.matchedWaypoints.get(i);
                if (wt.correctedpoint != null) {
                    final OsmNodeNamed n = new OsmNodeNamed(wt.correctedpoint);
                    n.name = wt.name + "_corr";
                    formatWaypointGpx(sb, n, "shaping");
                }
            }
            sb.append("\n");
        }
        sb.append(" <trk>\n");
        if (turnInstructionMode == 9
                || turnInstructionMode == 2
                || turnInstructionMode == 8
                || turnInstructionMode == 4) { // Locus, comment, cruise, brouter style
            sb.append("  <src>").append(t.name).append("</src>\n");
            sb.append("  <type>").append(t.voiceHints.getTransportMode()).append("</type>\n");
        } else {
            sb.append("  <name>").append(t.name).append("</name>\n");
        }

        if (turnInstructionMode == 7) {
            sb.append("  <extensions>\n");
            sb.append("   <locus:rteComputeType>").append("" + t.voiceHints.getLocusRouteType()).append("</locus:rteComputeType>\n");
            sb.append("   <locus:rteSimpleRoundabouts>1</locus:rteSimpleRoundabouts>\n");
            sb.append("  </extensions>\n");
        }


        // all points
        sb.append("  <trkseg>\n");
        String lastway = "";
        boolean bNextDirect = false;
        OsmPathElement nn = null;

        for (int idx = 0; idx < t.nodes.size(); idx++) {
            final OsmPathElement n = t.nodes.get(idx);
            String sele = n.getSElev() == Short.MIN_VALUE ? "" : "<ele>" + n.getElev() + "</ele>";
            final VoiceHint hint = t.getVoiceHint(idx);
            final MatchedWaypoint mwpt = t.getMatchedWaypoint(idx);

            if (t.showTime) {
                sele += "<time>" + getFormattedTime3(n.getTime()) + "</time>";
            }
            if ((turnInstructionMode == 8) && (mwpt != null && !mwpt.name.startsWith("via") && !mwpt.name.startsWith("from") && !mwpt.name.startsWith("to"))) {
                sele += "<name>" + mwpt.name + "</name>";
            }
            boolean bNeedHeader = false;
            if (turnInstructionMode == 9) { // trkpt/sym style

                if (hint != null) {

                    if (mwpt != null &&
                            !mwpt.name.startsWith("via") && !mwpt.name.startsWith("from") && !mwpt.name.startsWith("to")) {
                        sele += "<name>" + mwpt.name + "</name>";
                    }
                    sele += "<desc>" + hint.getCruiserMessageString() + "</desc>";
                    sele += "<sym>" + hint.getCommandString(hint.cmd, turnInstructionMode) + "</sym>";
                    if (mwpt != null) {
                        if (mwpt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_MEETING) {
                            sele += "<type>via</type>";
                        } else {
                            sele += "<type>shaping</type>";
                        }
                    }
                    sele += "<extensions>";
                    if (t.showspeed) {
                        double speed = 0;
                        if (nn != null) {
                            final int dist = n.calcDistance(nn);
                            final float dt = n.getTime() - nn.getTime();
                            if (dt != 0.f) {
                                speed = ((3.6f * dist) / dt + 0.5);
                            }
                        }
                        sele += "<brouter:speed>" + (((int) (speed * 10)) / 10.f) + "</brouter:speed>";
                    }

                    sele += "<brouter:voicehint>" + hint.getCommandString(turnInstructionMode) + ";" + (int) (hint.distanceToNext) + "," + hint.formatGeometry() + "</brouter:voicehint>";
                    if (n.message != null && n.message.wayKeyValues != null && !n.message.wayKeyValues.equals(lastway)) {
                        sele += "<brouter:way>" + n.message.wayKeyValues + "</brouter:way>";
                        lastway = n.message.wayKeyValues;
                    }
                    if (n.message != null && n.message.nodeKeyValues != null) {
                        sele += "<brouter:node>" + n.message.nodeKeyValues + "</brouter:node>";
                    }
                    sele += "</extensions>";

                }
                if (idx == 0 && hint == null) {
                    if (mwpt != null && mwpt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT) {
                        sele += "<desc>beeline</desc>";
                    } else {
                        sele += "<desc>start</desc>";
                    }
                    sele += "<type>via</type>";

                } else if (idx == t.nodes.size() - 1 && hint == null) {

                    sele += "<desc>end</desc>";
                    sele += "<type>Via</type>";

                } else {
                    if (mwpt != null && hint == null) {
                        if (mwpt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT) {
                            // bNextDirect = true;
                            sele += "<desc>beeline</desc>";
                        } else {
                            sele += "<desc>" + mwpt.name + "</desc>";
                        }
                        if (mwpt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_MEETING) {
                            sele += "<type>via</type>";
                        } else {
                            sele += "<type>shaping</type>";
                        }
                        bNextDirect = false;
                    }
                }


                if (hint == null) {
                    bNeedHeader = (t.showspeed || (n.message != null && n.message.wayKeyValues != null && !n.message.wayKeyValues.equals(lastway))) ||
                            (n.message != null && n.message.nodeKeyValues != null);
                    if (bNeedHeader) {
                        sele += "<extensions>";
                        if (t.showspeed) {
                            double speed = 0;
                            if (nn != null) {
                                final int dist = n.calcDistance(nn);
                                final float dt = n.getTime() - nn.getTime();
                                if (dt != 0.f) {
                                    speed = ((3.6f * dist) / dt + 0.5);
                                }
                            }
                            sele += "<brouter:speed>" + (((int) (speed * 10)) / 10.f) + "</brouter:speed>";
                        }
                        if (n.message != null && n.message.wayKeyValues != null && !n.message.wayKeyValues.equals(lastway)) {
                            sele += "<brouter:way>" + n.message.wayKeyValues + "</brouter:way>";
                            lastway = n.message.wayKeyValues;
                        }
                        if (n.message != null && n.message.nodeKeyValues != null) {
                            sele += "<brouter:node>" + n.message.nodeKeyValues + "</brouter:node>";
                        }
                        sele += "</extensions>";
                    }
                }
            }

            if (turnInstructionMode == 2) { // locus style new
                if (hint != null) {
                    if (mwpt != null) {
                        if (!mwpt.name.startsWith("via") && !mwpt.name.startsWith("from") && !mwpt.name.startsWith("to") && !mwpt.name.startsWith("rt")) {
                            sele += "<name>" + mwpt.name + "</name>";
                        }
                        if (mwpt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT && bNextDirect) {
                            sele += "<src>" + hint.getLocusSymbolString() + "</src><sym>pass_place</sym><type>Shaping</type>";
                            // bNextDirect = false;
                        } else if (mwpt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT) {
                            if (idx == 0) {
                                sele += "<sym>pass_place</sym><type>Via</type>";
                            } else {
                                sele += "<sym>pass_place</sym><type>Shaping</type>";
                            }
                            bNextDirect = true;
                        } else if (bNextDirect) {
                            sele += "<src>beeline</src><sym>" + hint.getLocusSymbolString() + "</sym><type>Shaping</type>";
                            bNextDirect = false;
                        } else {
                            sele += "<sym>" + hint.getLocusSymbolString() + "</sym><type>Via</type>";
                        }
                    } else {
                        sele += "<sym>" + hint.getLocusSymbolString() + "</sym>";
                    }
                } else {
                    if (idx == 0 && hint == null) {

                        final int pos = sele.indexOf("<sym");
                        if (pos != -1) {
                            sele = sele.substring(0, pos);
                        }
                        if (mwpt != null && !mwpt.name.startsWith("from")) {
                            sele += "<name>" + mwpt.name + "</name>";
                        }
                        if (mwpt != null && mwpt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT) {
                            bNextDirect = true;
                        }
                        sele += "<sym>pass_place</sym>";
                        sele += "<type>Via</type>";

                    } else if (idx == t.nodes.size() - 1 && hint == null) {

                        final int pos = sele.indexOf("<sym");
                        if (pos != -1) {
                            sele = sele.substring(0, pos);
                        }
                        if (mwpt != null && mwpt.name != null && !mwpt.name.startsWith("to")) {
                            sele += "<name>" + mwpt.name + "</name>";
                        }
                        if (bNextDirect) {
                            sele += "<src>beeline</src>";
                        }
                        sele += "<sym>pass_place</sym>";
                        sele += "<type>Via</type>";

                    } else {
                        if (mwpt != null) {
                            if (!mwpt.name.startsWith("via") && !mwpt.name.startsWith("from") && !mwpt.name.startsWith("to") && !mwpt.name.startsWith("rt")) {
                                sele += "<name>" + mwpt.name + "</name>";
                            }
                            if (mwpt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT && bNextDirect) {
                                sele += "<src>beeline</src><sym>pass_place</sym><type>Shaping</type>";
                            } else if (mwpt.wpttype == MatchedWaypoint.WAYPOINT_TYPE_DIRECT) {
                                if (idx == 0) {
                                    sele += "<sym>pass_place</sym><type>Via</type>";
                                } else {
                                    sele += "<sym>pass_place</sym><type>Shaping</type>";
                                }
                                bNextDirect = true;
                            } else if (bNextDirect) {
                                sele += "<src>beeline</src><sym>pass_place</sym><type>Shaping</type>";
                                bNextDirect = false;
                            } else if (mwpt.name.startsWith("via") ||
                                    mwpt.name.startsWith("from") ||
                                    mwpt.name.startsWith("to") ||
                                    mwpt.name.startsWith("rt")) {
                                if (bNextDirect) {
                                    sele += "<src>beeline</src><sym>pass_place</sym><type>Shaping</type>";
                                } else {
                                    sele += "<sym>pass_place</sym><type>Shaping</type>";
                                }
                                bNextDirect = false;
                            } else {
                                sele += "<name>" + mwpt.name + "</name>";
                                sele += "<sym>pass_place</sym><type>Via</type>";
                            }
                        }
                    }
                }
            }
            sb.append("   <trkpt lon=\"").append(formatILon(n.getILon())).append("\" lat=\"")
                    .append(formatILat(n.getILat())).append("\">").append(sele).append("</trkpt>\n");

            nn = n;
        }

        sb.append("  </trkseg>\n");
        sb.append(" </trk>\n");
        sb.append("</gpx>\n");

        return sb.toString();
    }

    public String formatAsWaypoint(OsmNodeNamed n) {
        try {
            final StringWriter sw = new StringWriter(8192);
            final BufferedWriter bw = new BufferedWriter(sw);
            formatGpxHeader(bw);
            formatWaypointGpx(bw, n, null);
            formatGpxFooter(bw);
            bw.close();
            sw.close();
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void formatGpxHeader(BufferedWriter sb) throws IOException {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gpx \n");
        sb.append(" xmlns=\"http://www.topografix.com/GPX/1/1\" \n");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
        sb.append(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\" \n");
        sb.append(" creator=\"BRouter-" + BRouterConstants.version + "\" version=\"1.1\">\n");
    }

    public void formatGpxFooter(BufferedWriter sb) throws IOException {
        sb.append("</gpx>\n");
    }

    public void formatWaypointGpx(BufferedWriter sb, OsmNodeNamed n, String type) throws IOException {
        sb.append(" <wpt lon=\"").append(formatILon(n.ilon)).append("\" lat=\"")
                .append(formatILat(n.ilat)).append("\">");
        if (n.getSElev() != Short.MIN_VALUE) {
            sb.append("<ele>").append("" + n.getElev()).append("</ele>");
        }
        if (n.name != null) {
            sb.append("<name>").append(StringUtils.escapeXml10(n.name)).append("</name>");
        }
        if (n.nodeDescription != null && rc != null) {
            sb.append("<desc>").append(rc.expctxWay.getKeyValueDescription(false, n.nodeDescription)).append("</desc>");
        }
        if (type != null) {
            sb.append("<type>").append(type).append("</type>");
        }
        sb.append("</wpt>\n");
    }

    public void formatWaypointGpx(BufferedWriter sb, MatchedWaypoint wp, String type) throws IOException {
        sb.append(" <wpt lon=\"").append(formatILon(wp.waypoint.ilon)).append("\" lat=\"")
                .append(formatILat(wp.waypoint.ilat)).append("\">");
        if (wp.waypoint.getSElev() != Short.MIN_VALUE) {
            sb.append("<ele>").append("" + wp.waypoint.getElev()).append("</ele>");
        }
        if (wp.name != null) {
            sb.append("<name>").append(StringUtils.escapeXml10(wp.name)).append("</name>");
        }
        if (type != null) {
            sb.append("<type>").append(type).append("</type>");
        }
        sb.append("</wpt>\n");
    }

    public OsmTrack read(String filename) throws Exception {
        final File f = new File(filename);
        if (!f.exists()) {
            return null;
        }
        final OsmTrack track = new OsmTrack();
        final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

        for (; ; ) {
            final String line = br.readLine();
            if (line == null) {
                break;
            }

            int idx0 = line.indexOf("<trkpt ");
            if (idx0 >= 0) {
                idx0 = line.indexOf(" lon=\"");
                idx0 += 6;
                final int idx1 = line.indexOf('"', idx0);
                final int ilon = (int) ((Double.parseDouble(line.substring(idx0, idx1)) + 180.) * 1000000. + 0.5);
                int idx2 = line.indexOf(" lat=\"");
                if (idx2 < 0) {
                    continue;
                }
                idx2 += 6;
                final int idx3 = line.indexOf('"', idx2);
                final int ilat = (int) ((Double.parseDouble(line.substring(idx2, idx3)) + 90.) * 1000000. + 0.5);
                track.nodes.add(OsmPathElement.create(ilon, ilat, (short) 0, null));
            }
        }
        br.close();
        return track;
    }

}
