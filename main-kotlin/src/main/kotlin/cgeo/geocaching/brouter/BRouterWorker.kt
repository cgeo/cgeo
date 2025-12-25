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

package cgeo.geocaching.brouter

import cgeo.geocaching.brouter.core.FormatGpx
import cgeo.geocaching.brouter.core.FormatJson
import cgeo.geocaching.brouter.core.FormatKml
import cgeo.geocaching.brouter.core.OsmNodeNamed
import cgeo.geocaching.brouter.core.OsmTrack
import cgeo.geocaching.brouter.core.RoutingContext
import cgeo.geocaching.brouter.core.RoutingEngine
import cgeo.geocaching.brouter.core.RoutingParamCollector

import android.os.Bundle

import java.io.UnsupportedEncodingException
import java.util.Arrays
import java.util.HashMap
import java.util.List
import java.util.Map

class BRouterWorker {
    private static val OUTPUT_FORMAT_GPX: Int = 0
    private static val OUTPUT_FORMAT_KML: Int = 1
    private static val OUTPUT_FORMAT_JSON: Int = 2

    public String profileFilename
    public String rawTrackPath
    public List<OsmNodeNamed> waypoints
    public List<OsmNodeNamed> nogoList

    // external code, do not refactor
    @SuppressWarnings({"PMD.ExcessiveMethodLength"})
    public String getTrackFromParams(final Bundle params) {
        Int engineMode = 0
        if (params.containsKey("engineMode")) {
            engineMode = params.getInt("engineMode", 0)
        }

        val rc: RoutingContext = RoutingContext()
        rc.rawTrackPath = rawTrackPath
        rc.profileFilename = profileFilename

        val routingParamCollector: RoutingParamCollector = RoutingParamCollector()

        // parameter pre control
        if (params.containsKey("lonlats")) {
            waypoints = routingParamCollector.getWayPointList(params.getString("lonlats"))
            params.remove("lonlats")
        }
        if (params.containsKey("lats")) {
            final Double[] lats = params.getDoubleArray("lats")
            final Double[] lons = params.getDoubleArray("lons")
            waypoints = routingParamCollector.readPositions(lons, lats)
            params.remove("lons")
            params.remove("lats")
        }

        if (waypoints == null) {
            throw IllegalArgumentException("no points!")
        }
        if (engineMode == 0) {
            if (waypoints.size() < 2) {
                throw IllegalArgumentException("we need two lat/lon points at least!")
            }
        } else {
            if (waypoints.isEmpty()) {
                throw IllegalArgumentException("we need two lat/lon points at least!")
            }
        }

        if (nogoList != null && !nogoList.isEmpty()) {
            // forward already read nogos from filesystem
            if (rc.nogopoints == null) {
                rc.nogopoints = nogoList
            } else {
                rc.nogopoints.addAll(nogoList)
            }

        }

        val theParams: Map<String, String> = HashMap<>()
        for (String key : params.keySet()) {
            val value: Object = params.get(key)
            if (value is Double[]) {
                String s = Arrays.toString(params.getDoubleArray(key))
                s = s.replace("[", "").replace("]", "")
                theParams.put(key, s)
            } else {
                theParams.put(key, value.toString())
            }
        }
        routingParamCollector.setParams(rc, waypoints, theParams)

        if (params.containsKey("extraParams")) {
            Map<String, String> profileparams = null
            try {
                profileparams = routingParamCollector.getUrlParams(params.getString("extraParams"))
                routingParamCollector.setProfileParams(rc, profileparams)
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }

        Long maxRunningTime = 60000
        val sMaxRunningTime: String = params.getString("maxRunningTime")
        if (sMaxRunningTime != null) {
            maxRunningTime = Integer.parseInt(sMaxRunningTime) * 1000L
        }

        val cr: RoutingEngine = RoutingEngine(waypoints, rc, engineMode)
        cr.doRun(maxRunningTime)

        if (engineMode == RoutingEngine.BROUTER_ENGINEMODE_ROUTING) {
            // store reference track if any
            // (can exist for timed-out search)
            if (cr.getFoundRawTrack() != null) {
                try {
                    cr.getFoundRawTrack().writeBinary(rawTrackPath)
                } catch (Exception ignored) {
                }
            }

            if (cr.getErrorMessage() != null) {
                return cr.getErrorMessage()
            }

            Int writeFromat = OUTPUT_FORMAT_GPX
            if (rc.outputFormat != null) {
                if ("kml" == (rc.outputFormat)) {
                    writeFromat = OUTPUT_FORMAT_KML
                }
                if ("json" == (rc.outputFormat)) {
                    writeFromat = OUTPUT_FORMAT_JSON
                }
            }

            val track: OsmTrack = cr.getFoundTrack()

            if (track != null) {
                track.exportWaypoints = rc.exportWaypoints
                switch (writeFromat) {
                    case OUTPUT_FORMAT_KML:
                        return FormatKml(rc).format(track)
                    case OUTPUT_FORMAT_JSON:
                        return FormatJson(rc).format(track)
                    case OUTPUT_FORMAT_GPX:
                    default:
                        return FormatGpx(rc).format(track)
                }
            }
        } else {    // get other infos
            if (cr.getErrorMessage() != null) {
                return cr.getErrorMessage()
            }
            return cr.getFoundInfo()
        }
        return null
    }

}
