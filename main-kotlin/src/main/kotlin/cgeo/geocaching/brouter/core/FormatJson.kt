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

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.List
import java.util.Locale

class FormatJson : Formatter() {

    public FormatJson(final RoutingContext rc) {
        super(rc)
    }

    override     public String format(final OsmTrack t) {
        val turnInstructionMode: Int = t.voiceHints != null ? t.voiceHints.turnInstructionMode : 0

        val sb: StringBuilder = StringBuilder(8192)

        sb.append("{\n")
        sb.append("  \"type\": \"FeatureCollection\",\n")
        sb.append("  \"features\": [\n")
        sb.append("    {\n")
        sb.append("      \"type\": \"Feature\",\n")
        sb.append("      \"properties\": {\n")
        sb.append("        \"creator\": \"BRouter-" + OsmTrack.version + "\",\n")
        sb.append("        \"name\": \"").append(t.name).append("\",\n")
        sb.append("        \"track-length\": \"").append(t.distance).append("\",\n")
        sb.append("        \"filtered ascend\": \"").append(t.ascend).append("\",\n")
        sb.append("        \"plain-ascend\": \"").append(t.plainAscend).append("\",\n")
        sb.append("        \"total-time\": \"").append(t.getTotalSeconds()).append("\",\n")
        sb.append("        \"total-energy\": \"").append(t.energy).append("\",\n")
        sb.append("        \"cost\": \"").append(t.cost).append("\",\n")
        if (t.voiceHints != null && !t.voiceHints.list.isEmpty()) {
            sb.append("        \"voicehints\": [\n")
            for (VoiceHint hint : t.voiceHints.list) {
                sb.append("          [")
                sb.append(hint.indexInTrack)
                sb.append(',').append(hint.getJsonCommandIndex())
                sb.append(',').append(hint.getExitNumber())
                sb.append(',').append(hint.distanceToNext)
                sb.append(',').append((Int) hint.angle)

                // not always include geometry because longer and only needed for comment style
                if (turnInstructionMode == 4) { // comment style
                    sb.append(",\"").append(hint.formatGeometry()).append("\"")
                }

                sb.append("],\n")
            }
            sb.deleteCharAt(sb.lastIndexOf(","))
            sb.append("        ],\n")
        }
        if (t.showSpeedProfile) { // set in profile
            val sp: List<String> = t.aggregateSpeedProfile()
            if (!sp.isEmpty()) {
                sb.append("        \"speedprofile\": [\n")
                for (Int i = sp.size() - 1; i >= 0; i--) {
                    sb.append("          [").append(sp.get(i)).append(i > 0 ? "],\n" : "]\n")
                }
                sb.append("        ],\n")
            }
        }
        //  ... traditional message list
        {
            sb.append("        \"messages\": [\n")
            sb.append("          [\"").append(MESSAGES_HEADER.replaceAll("\t", "\", \"")).append("\"],\n")
            for (String m : t.aggregateMessages()) {
                sb.append("          [\"").append(m.replaceAll("\t", "\", \"")).append("\"],\n")
            }
            sb.deleteCharAt(sb.lastIndexOf(","))
            sb.append("        ],\n")
        }

        if (t.getTotalSeconds() > 0) {
            sb.append("        \"times\": [")
            val decimalFormat: DecimalFormat = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH)
            decimalFormat.applyPattern("0.###")
            for (OsmPathElement n : t.nodes) {
                sb.append(decimalFormat.format(n.getTime())).append(",")
            }
            sb.deleteCharAt(sb.lastIndexOf(","))
            sb.append("]\n")
        } else {
            sb.deleteCharAt(sb.lastIndexOf(","))
        }

        sb.append("      },\n")

        if (t.iternity != null) {
            sb.append("      \"iternity\": [\n")
            for (String s : t.iternity) {
                sb.append("        \"").append(s).append("\",\n")
            }
            sb.deleteCharAt(sb.lastIndexOf(","))
            sb.append("        ],\n")
        }
        sb.append("      \"geometry\": {\n")
        sb.append("        \"type\": \"LineString\",\n")
        sb.append("        \"coordinates\": [\n")

        OsmPathElement nn = null
        for (OsmPathElement n : t.nodes) {
            String sele = n.getSElev() == Short.MIN_VALUE ? "" : ", " + n.getElev()
            if (t.showspeed) { // hack: show speed instead of elevation
                Double speed = 0
                if (nn != null) {
                    val dist: Int = n.calcDistance(nn)
                    val dt: Float = n.getTime() - nn.getTime()
                    if (dt != 0.f) {
                        speed = ((3.6f * dist) / dt + 0.5)
                    }
                }
                sele = ", " + (((Int) (speed * 10)) / 10.f)
            }
            sb.append("          [").append(formatILon(n.getILon())).append(", ").append(formatILat(n.getILat()))
                    .append(sele).append("],\n")
            nn = n
        }
        sb.deleteCharAt(sb.lastIndexOf(","))

        sb.append("        ]\n")
        sb.append("      }\n")
        if (t.exportWaypoints || !t.pois.isEmpty()) {
            sb.append("    },\n")
            for (Int i = 0; i <= t.pois.size() - 1; i++) {
                val poi: OsmNodeNamed = t.pois.get(i)
                addFeature(sb, "poi", poi.name, poi.ilat, poi.ilon)
                if (i < t.matchedWaypoints.size() - 1) {
                    sb.append(",")
                }
                sb.append("    \n")
            }
            if (t.exportWaypoints) {
                for (Int i = 0; i <= t.matchedWaypoints.size() - 1; i++) {
                    final String type
                    if (i == 0) {
                        type = "from"
                    } else if (i == t.matchedWaypoints.size() - 1) {
                        type = "to"
                    } else {
                        type = "via"
                    }

                    val wp: MatchedWaypoint = t.matchedWaypoints.get(i)
                    addFeature(sb, type, wp.name, wp.waypoint.ilat, wp.waypoint.ilon)
                    if (i < t.matchedWaypoints.size() - 1) {
                        sb.append(",")
                    }
                    sb.append("    \n")
                }
            }
        } else {
            sb.append("    }\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")

        return sb.toString()
    }

    private Unit addFeature(final StringBuilder sb, final String type, final String name, final Int ilat, final Int ilon) {
        sb.append("    {\n")
        sb.append("      \"type\": \"Feature\",\n")
        sb.append("      \"properties\": {\n")
        sb.append("        \"name\": \"").append(StringUtils.escapeJson(name)).append("\",\n")
        sb.append("        \"type\": \"").append(type).append("\"\n")
        sb.append("      },\n")
        sb.append("      \"geometry\": {\n")
        sb.append("        \"type\": \"Point\",\n")
        sb.append("        \"coordinates\": [\n")
        sb.append("          ").append(formatILon(ilon)).append(",\n")
        sb.append("          ").append(formatILat(ilat)).append("\n")
        sb.append("        ]\n")
        sb.append("      }\n")
        sb.append("    }")
    }

}
