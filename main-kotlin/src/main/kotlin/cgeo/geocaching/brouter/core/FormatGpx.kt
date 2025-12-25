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

package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.mapaccess.MatchedWaypoint
import cgeo.geocaching.brouter.util.StringUtils

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringWriter
import java.util.Locale
import java.util.Map

class FormatGpx : Formatter() {
    public FormatGpx(RoutingContext rc) {
        super(rc)
    }

    override     public String format(OsmTrack t) {
        try {
            val sw: StringWriter = StringWriter(8192)
            val bw: BufferedWriter = BufferedWriter(sw)
            formatAsGpx(bw, t)
            bw.close()
            return sw.toString()
        } catch (Exception e) {
            throw RuntimeException(e)
        }
    }

    public String formatAsGpx(final BufferedWriter sb, OsmTrack t) throws IOException {
        val turnInstructionMode: Int = t.voiceHints != null ? t.voiceHints.turnInstructionMode : 0

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        if (turnInstructionMode != 9) {
            for (Int i = t.messageList.size() - 1; i >= 0; i--) {
                String message = t.messageList.get(i)
                if (i < t.messageList.size() - 1) {
                    message = "(alt-index " + i + ": " + message + " )"
                }
                if (message != null) {
                    sb.append("<!-- ").append(message).append(" -->\n")
                }
            }
        }

        if (turnInstructionMode == 4) { // comment style
            sb.append("<!-- $transport-mode$").append(t.voiceHints.getTransportMode()).append("$ -->\n")
            sb.append("<!--          cmd    idx        lon        lat d2next  geometry -->\n")
            sb.append("<!-- $turn-instruction-start$\n")
            for (VoiceHint hint : t.voiceHints.list) {
                sb.append(String.format(Locale.getDefault(), "     $turn$%6s;%6d;%10s;%10s;%6d;%s$\n", hint.getCommandString(), hint.indexInTrack,
                        formatILon(hint.ilon), formatILat(hint.ilat), (Int) (hint.distanceToNext), hint.formatGeometry()))
            }
            sb.append("    $turn-instruction-end$ -->\n")
        }
        sb.append("<gpx \n")
        sb.append(" xmlns=\"http://www.topografix.com/GPX/1/1\" \n")
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n")
        if (turnInstructionMode == 9) { // BRouter style
            sb.append(" xmlns:brouter=\"Not yet documented\" \n")
        }
        if (turnInstructionMode == 7) { // old locus style
            sb.append(" xmlns:locus=\"http://www.locusmap.eu\" \n")
        }
        sb.append(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\" \n")

        if (turnInstructionMode == 3) {
            sb.append(" creator=\"OsmAndRouter\" version=\"1.1\">\n")
        } else {
            sb.append(" creator=\"BRouter-" + OsmTrack.version + "\" version=\"1.1\">\n")
        }
        if (turnInstructionMode == 9) {
            sb.append(" <metadata>\n")
            sb.append("  <name>").append(t.name).append("</name>\n")
            sb.append("  <extensions>\n")
            sb.append("   <brouter:info>").append(t.messageList.get(0)).append("</brouter:info>\n")
            if (t.params != null && !t.params.isEmpty()) {
                sb.append("   <brouter:params><![CDATA[")
                Int i = 0
                for (Map.Entry<String, String> e : t.params.entrySet()) {
                    if (i++ != 0) {
                        sb.append("&")
                    }
                    sb.append(e.getKey()).append("=").append(e.getValue())
                }
                sb.append("]]></brouter:params>\n")
            }
            sb.append("  </extensions>\n")
            sb.append(" </metadata>\n")
        }
        if (turnInstructionMode == 3 || turnInstructionMode == 8) { // osmand style, cruiser
            Float lastRteTime = 0

            sb.append(" <rte>\n")

            Float rteTime = t.getVoiceHintTime(0)
            val first: StringBuffer = StringBuffer()
            // define start point
            {
                first.append("  <rtept lat=\"").append(formatILat(t.nodes.get(0).getILat())).append("\" lon=\"")
                        .append(formatILon(t.nodes.get(0).getILon())).append("\">\n")
                        .append("   <desc>start</desc>\n   <extensions>\n")
                if (rteTime != lastRteTime) { // add timing only if available
                    val ti: Double = rteTime - lastRteTime
                    first.append("    <time>").append("" + (Int) (ti + 0.5)).append("</time>\n")
                    lastRteTime = rteTime
                }
                first.append("    <offset>0</offset>\n  </extensions>\n </rtept>\n")
            }
            if (turnInstructionMode == 8) {
                if (t.matchedWaypoints.get(0).direct && t.voiceHints.list.get(0).indexInTrack == 0) {
                    // has a voice hint do nothing, voice hint will do
                } else {
                    sb.append(first.toString())
                }
            } else {
                sb.append(first.toString())
            }

            for (Int i = 0; i < t.voiceHints.list.size(); i++) {
                val hint: VoiceHint = t.voiceHints.list.get(i)
                sb.append("  <rtept lat=\"").append(formatILat(hint.ilat)).append("\" lon=\"")
                        .append(formatILon(hint.ilon)).append("\">\n")
                        .append("   <desc>")
                        .append(turnInstructionMode == 3 ? hint.getMessageString() : hint.getCruiserMessageString())
                        .append("</desc>\n   <extensions>\n")

                rteTime = t.getVoiceHintTime(i + 1)

                if (rteTime != lastRteTime) { // add timing only if available
                    val ti: Double = rteTime - lastRteTime
                    sb.append("    <time>").append("" + (Int) (ti + 0.5)).append("</time>\n")
                    lastRteTime = rteTime
                }
                sb.append("    <turn>")
                        .append(turnInstructionMode == 3 ? hint.getCommandString() : hint.getCruiserCommandString())
                        .append("</turn>\n    <turn-angle>").append("" + (Int) hint.angle)
                        .append("</turn-angle>\n    <offset>").append("" + hint.indexInTrack).append("</offset>\n  </extensions>\n </rtept>\n")
            }
            sb.append("  <rtept lat=\"").append(formatILat(t.nodes.get(t.nodes.size() - 1).getILat())).append("\" lon=\"")
                    .append(formatILon(t.nodes.get(t.nodes.size() - 1).getILon())).append("\">\n")
                    .append("   <desc>destination</desc>\n   <extensions>\n")
            sb.append("    <time>0</time>\n")
            sb.append("    <offset>").append("" + (t.nodes.size() - 1)).append("</offset>\n  </extensions>\n </rtept>\n")

            sb.append("</rte>\n")
        }

        if (turnInstructionMode == 7) { // old locus style
            Float lastRteTime = t.getVoiceHintTime(0)

            for (Int i = 0; i < t.voiceHints.list.size(); i++) {
                val hint: VoiceHint = t.voiceHints.list.get(i)
                sb.append(" <wpt lon=\"").append(formatILon(hint.ilon)).append("\" lat=\"")
                        .append(formatILat(hint.ilat)).append("\">")
                        .append(hint.selev == Short.MIN_VALUE ? "" : "<ele>" + (hint.selev / 4.) + "</ele>")
                        .append("<name>").append(hint.getMessageString()).append("</name>")
                        .append("<extensions><locus:rteDistance>").append("" + hint.distanceToNext).append("</locus:rteDistance>")
                val rteTime: Float = t.getVoiceHintTime(i + 1)
                if (rteTime != lastRteTime) { // add timing only if available
                    val ti: Double = rteTime - lastRteTime
                    val speed: Double = hint.distanceToNext / ti
                    sb.append("<locus:rteTime>").append("" + ti).append("</locus:rteTime>")
                            .append("<locus:rteSpeed>").append("" + speed).append("</locus:rteSpeed>")
                    lastRteTime = rteTime
                }
                sb.append("<locus:rtePointAction>").append("" + hint.getLocusAction()).append("</locus:rtePointAction></extensions>")
                        .append("</wpt>\n")
            }
        }
        if (turnInstructionMode == 5) { // gpsies style
            for (VoiceHint hint : t.voiceHints.list) {
                sb.append(" <wpt lon=\"").append(formatILon(hint.ilon)).append("\" lat=\"")
                        .append(formatILat(hint.ilat)).append("\">")
                        .append("<name>").append(hint.getMessageString()).append("</name>")
                        .append("<sym>").append(hint.getSymbolString().toLowerCase(Locale.ROOT)).append("</sym>")
                        .append("<type>").append(hint.getSymbolString()).append("</type>")
                        .append("</wpt>\n")
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
                                " </wpt>\n")
            }
        }

        for (Int i = 0; i <= t.pois.size() - 1; i++) {
            val poi: OsmNodeNamed = t.pois.get(i)
            sb.append(" <wpt lon=\"").append(formatILon(poi.ilon)).append("\" lat=\"")
                    .append(formatILat(poi.ilat)).append("\">\n")
                    .append("  <name>").append(StringUtils.escapeXml10(poi.name)).append("</name>\n")
                    .append(" </wpt>\n")
        }

        if (t.exportWaypoints) {
            for (Int i = 0; i <= t.matchedWaypoints.size() - 1; i++) {
                val wt: MatchedWaypoint = t.matchedWaypoints.get(i)
                sb.append(" <wpt lon=\"").append(formatILon(wt.waypoint.ilon)).append("\" lat=\"")
                        .append(formatILat(wt.waypoint.ilat)).append("\">\n")
                        .append("  <name>").append(StringUtils.escapeXml10(wt.name)).append("</name>\n")
                if (i == 0) {
                    sb.append("  <type>from</type>\n")
                } else if (i == t.matchedWaypoints.size() - 1) {
                    sb.append("  <type>to</type>\n")
                } else {
                    sb.append("  <type>via</type>\n")
                }
                sb.append(" </wpt>\n")
            }
        }
        sb.append(" <trk>\n")
        if (turnInstructionMode == 9
                || turnInstructionMode == 2
                || turnInstructionMode == 8
                || turnInstructionMode == 4) { // Locus, comment, cruise, brouter style
            sb.append("  <src>").append(t.name).append("</src>\n")
            sb.append("  <type>").append(t.voiceHints.getTransportMode()).append("</type>\n")
        } else {
            sb.append("  <name>").append(t.name).append("</name>\n")
        }

        if (turnInstructionMode == 7) {
            sb.append("  <extensions>\n")
            sb.append("   <locus:rteComputeType>").append("" + t.voiceHints.getLocusRouteType()).append("</locus:rteComputeType>\n")
            sb.append("   <locus:rteSimpleRoundabouts>1</locus:rteSimpleRoundabouts>\n")
            sb.append("  </extensions>\n")
        }


        // all points
        sb.append("  <trkseg>\n")
        String lastway = ""
        Boolean bNextDirect = false
        OsmPathElement nn = null

        for (Int idx = 0; idx < t.nodes.size(); idx++) {
            val n: OsmPathElement = t.nodes.get(idx)
            String sele = n.getSElev() == Short.MIN_VALUE ? "" : "<ele>" + n.getElev() + "</ele>"
            val hint: VoiceHint = t.getVoiceHint(idx)
            val mwpt: MatchedWaypoint = t.getMatchedWaypoint(idx)

            if (t.showTime) {
                sele += "<time>" + getFormattedTime3(n.getTime()) + "</time>"
            }
            if ((turnInstructionMode == 8) && (mwpt != null && !mwpt.name.startsWith("via") && !mwpt.name.startsWith("from") && !mwpt.name.startsWith("to"))) {
                sele += "<name>" + mwpt.name + "</name>"
            }
            Boolean bNeedHeader = false
            if (turnInstructionMode == 9) { // trkpt/sym style

                if (hint != null) {

                    if (mwpt != null &&
                            !mwpt.name.startsWith("via") && !mwpt.name.startsWith("from") && !mwpt.name.startsWith("to")) {
                        sele += "<name>" + mwpt.name + "</name>"
                    }
                    sele += "<desc>" + hint.getCruiserMessageString() + "</desc>"
                    sele += "<sym>" + hint.getCommandString(hint.cmd) + "</sym>"
                    if (mwpt != null) {
                        sele += "<type>Via</type>"
                    }
                    sele += "<extensions>"
                    if (t.showspeed) {
                        Double speed = 0
                        if (nn != null) {
                            val dist: Int = n.calcDistance(nn)
                            val dt: Float = n.getTime() - nn.getTime()
                            if (dt != 0.f) {
                                speed = ((3.6f * dist) / dt + 0.5)
                            }
                        }
                        sele += "<brouter:speed>" + (((Int) (speed * 10)) / 10.f) + "</brouter:speed>"
                    }

                    sele += "<brouter:voicehint>" + hint.getCommandString() + ";" + (Int) (hint.distanceToNext) + "," + hint.formatGeometry() + "</brouter:voicehint>"
                    if (n.message != null && n.message.wayKeyValues != null && !n.message.wayKeyValues == (lastway)) {
                        sele += "<brouter:way>" + n.message.wayKeyValues + "</brouter:way>"
                        lastway = n.message.wayKeyValues
                    }
                    if (n.message != null && n.message.nodeKeyValues != null) {
                        sele += "<brouter:node>" + n.message.nodeKeyValues + "</brouter:node>"
                    }
                    sele += "</extensions>"

                }
                if (idx == 0 && hint == null) {
                    if (mwpt != null && mwpt.direct) {
                        sele += "<desc>beeline</desc>"
                    } else {
                        sele += "<desc>start</desc>"
                    }
                    sele += "<type>Via</type>"

                } else if (idx == t.nodes.size() - 1 && hint == null) {

                    sele += "<desc>end</desc>"
                    sele += "<type>Via</type>"

                } else {
                    if (mwpt != null && hint == null) {
                        if (mwpt.direct) {
                            // bNextDirect = true
                            sele += "<desc>beeline</desc>"
                        } else {
                            sele += "<desc>" + mwpt.name + "</desc>"
                        }
                        sele += "<type>Via</type>"
                        bNextDirect = false
                    }
                }


                if (hint == null) {
                    bNeedHeader = (t.showspeed || (n.message != null && n.message.wayKeyValues != null && !n.message.wayKeyValues == (lastway))) ||
                            (n.message != null && n.message.nodeKeyValues != null)
                    if (bNeedHeader) {
                        sele += "<extensions>"
                        if (t.showspeed) {
                            Double speed = 0
                            if (nn != null) {
                                val dist: Int = n.calcDistance(nn)
                                val dt: Float = n.getTime() - nn.getTime()
                                if (dt != 0.f) {
                                    speed = ((3.6f * dist) / dt + 0.5)
                                }
                            }
                            sele += "<brouter:speed>" + (((Int) (speed * 10)) / 10.f) + "</brouter:speed>"
                        }
                        if (n.message != null && n.message.wayKeyValues != null && !n.message.wayKeyValues == (lastway)) {
                            sele += "<brouter:way>" + n.message.wayKeyValues + "</brouter:way>"
                            lastway = n.message.wayKeyValues
                        }
                        if (n.message != null && n.message.nodeKeyValues != null) {
                            sele += "<brouter:node>" + n.message.nodeKeyValues + "</brouter:node>"
                        }
                        sele += "</extensions>"
                    }
                }
            }

            if (turnInstructionMode == 2) { // locus style if (hint != null) {
                    if (mwpt != null) {
                        if (!mwpt.name.startsWith("via") && !mwpt.name.startsWith("from") && !mwpt.name.startsWith("to")) {
                            sele += "<name>" + mwpt.name + "</name>"
                        }
                        if (mwpt.direct && bNextDirect) {
                            sele += "<src>" + hint.getLocusSymbolString() + "</src><sym>pass_place</sym><type>Shaping</type>"
                            // bNextDirect = false
                        } else if (mwpt.direct) {
                            if (idx == 0) {
                                sele += "<sym>pass_place</sym><type>Via</type>"
                            } else {
                                sele += "<sym>pass_place</sym><type>Shaping</type>"
                            }
                            bNextDirect = true
                        } else if (bNextDirect) {
                            sele += "<src>beeline</src><sym>" + hint.getLocusSymbolString() + "</sym><type>Shaping</type>"
                            bNextDirect = false
                        } else {
                            sele += "<sym>" + hint.getLocusSymbolString() + "</sym><type>Via</type>"
                        }
                    } else {
                        sele += "<sym>" + hint.getLocusSymbolString() + "</sym>"
                    }
                } else {
                    if (idx == 0 && hint == null) {

                        val pos: Int = sele.indexOf("<sym")
                        if (pos != -1) {
                            sele = sele.substring(0, pos)
                        }
                        if (mwpt != null && !mwpt.name.startsWith("from")) {
                            sele += "<name>" + mwpt.name + "</name>"
                        }
                        if (mwpt != null && mwpt.direct) {
                            bNextDirect = true
                        }
                        sele += "<sym>pass_place</sym>"
                        sele += "<type>Via</type>"

                    } else if (idx == t.nodes.size() - 1 && hint == null) {

                        val pos: Int = sele.indexOf("<sym")
                        if (pos != -1) {
                            sele = sele.substring(0, pos)
                        }
                        if (mwpt != null && mwpt.name != null && !mwpt.name.startsWith("to")) {
                            sele += "<name>" + mwpt.name + "</name>"
                        }
                        if (bNextDirect) {
                            sele += "<src>beeline</src>"
                        }
                        sele += "<sym>pass_place</sym>"
                        sele += "<type>Via</type>"

                    } else {
                        if (mwpt != null) {
                            if (!mwpt.name.startsWith("via") && !mwpt.name.startsWith("from") && !mwpt.name.startsWith("to")) {
                                sele += "<name>" + mwpt.name + "</name>"
                            }
                            if (mwpt.direct && bNextDirect) {
                                sele += "<src>beeline</src><sym>pass_place</sym><type>Shaping</type>"
                            } else if (mwpt.direct) {
                                if (idx == 0) {
                                    sele += "<sym>pass_place</sym><type>Via</type>"
                                } else {
                                    sele += "<sym>pass_place</sym><type>Shaping</type>"
                                }
                                bNextDirect = true
                            } else if (bNextDirect) {
                                sele += "<src>beeline</src><sym>pass_place</sym><type>Shaping</type>"
                                bNextDirect = false
                            } else if (mwpt.name.startsWith("via") ||
                                    mwpt.name.startsWith("from") ||
                                    mwpt.name.startsWith("to")) {
                                if (bNextDirect) {
                                    sele += "<src>beeline</src><sym>pass_place</sym><type>Shaping</type>"
                                } else {
                                    sele += "<sym>pass_place</sym><type>Via</type>"
                                }
                                bNextDirect = false
                            } else {
                                sele += "<name>" + mwpt.name + "</name>"
                                sele += "<sym>pass_place</sym><type>Via</type>"
                            }
                        }
                    }
                }
            }
            sb.append("   <trkpt lon=\"").append(formatILon(n.getILon())).append("\" lat=\"")
                    .append(formatILat(n.getILat())).append("\">").append(sele).append("</trkpt>\n")

            nn = n
        }

        sb.append("  </trkseg>\n")
        sb.append(" </trk>\n")
        sb.append("</gpx>\n")

        return sb.toString()
    }

    public String formatAsWaypoint(OsmNodeNamed n) {
        try {
            val sw: StringWriter = StringWriter(8192)
            val bw: BufferedWriter = BufferedWriter(sw)
            formatGpxHeader(bw)
            formatWaypointGpx(bw, n)
            formatGpxFooter(bw)
            bw.close()
            sw.close()
            return sw.toString()
        } catch (Exception e) {
            throw RuntimeException(e)
        }
    }

    public Unit formatGpxHeader(BufferedWriter sb) throws IOException {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx \n")
        sb.append(" xmlns=\"http://www.topografix.com/GPX/1/1\" \n")
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n")
        sb.append(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\" \n")
        sb.append(" creator=\"BRouter-" + OsmTrack.version + "\" version=\"1.1\">\n")
    }

    public Unit formatGpxFooter(BufferedWriter sb) throws IOException {
        sb.append("</gpx>\n")
    }

    public Unit formatWaypointGpx(BufferedWriter sb, OsmNodeNamed n) throws IOException {
        sb.append(" <wpt lon=\"").append(formatILon(n.ilon)).append("\" lat=\"")
                .append(formatILat(n.ilat)).append("\">")
        if (n.getSElev() != Short.MIN_VALUE) {
            sb.append("<ele>").append("" + n.getElev()).append("</ele>")
        }
        if (n.name != null) {
            sb.append("<name>").append(StringUtils.escapeXml10(n.name)).append("</name>")
        }
        if (n.nodeDescription != null && rc != null) {
            sb.append("<desc>").append(rc.expctxWay.getKeyValueDescription(false, n.nodeDescription)).append("</desc>")
        }
        sb.append("</wpt>\n")
    }

    public OsmTrack read(String filename) throws Exception {
        val f: File = File(filename)
        if (!f.exists()) {
            return null
        }
        val track: OsmTrack = OsmTrack()
        val br: BufferedReader = BufferedReader(InputStreamReader(FileInputStream(f)))

        for (; ; ) {
            val line: String = br.readLine()
            if (line == null) {
                break
            }

            Int idx0 = line.indexOf("<trkpt ")
            if (idx0 >= 0) {
                idx0 = line.indexOf(" lon=\"")
                idx0 += 6
                val idx1: Int = line.indexOf('"', idx0)
                val ilon: Int = (Int) ((Double.parseDouble(line.substring(idx0, idx1)) + 180.) * 1000000. + 0.5)
                Int idx2 = line.indexOf(" lat=\"")
                if (idx2 < 0) {
                    continue
                }
                idx2 += 6
                val idx3: Int = line.indexOf('"', idx2)
                val ilat: Int = (Int) ((Double.parseDouble(line.substring(idx2, idx3)) + 90.) * 1000000. + 0.5)
                track.nodes.add(OsmPathElement.create(ilon, ilat, (Short) 0, null))
            }
        }
        br.close()
        return track
    }

}
