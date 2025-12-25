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

/**
 * Container for routig configs
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.BRouterConstants
import cgeo.geocaching.brouter.expressions.BExpressionContext
import cgeo.geocaching.brouter.expressions.BExpressionContextNode
import cgeo.geocaching.brouter.expressions.BExpressionContextWay
import cgeo.geocaching.brouter.mapaccess.GeometryDecoder
import cgeo.geocaching.brouter.mapaccess.MatchedWaypoint
import cgeo.geocaching.brouter.mapaccess.OsmLink
import cgeo.geocaching.brouter.mapaccess.OsmNode
import cgeo.geocaching.brouter.util.CheapAngleMeter
import cgeo.geocaching.brouter.util.CheapRulerHelper

import java.util.ArrayList
import java.util.List
import java.util.Map

class RoutingContext {
    var alternativeIdx: Int = 0
    public String profileFilename
    public Long profileTimestamp
    public Map<String, String> keyValues
    public String rawTrackPath
    public BExpressionContextWay expctxWay
    public BExpressionContextNode expctxNode
    var geometryDecoder: GeometryDecoder = GeometryDecoder()
    var memoryclass: Int = 64
    public Boolean carMode
    public Boolean bikeMode
    public Boolean footMode
    public Boolean considerTurnRestrictions
    public Boolean processUnusedTags
    public Double pass1coefficient
    public Double pass2coefficient
    public Int elevationpenaltybuffer
    public Int elevationmaxbuffer
    public Int elevationbufferreduce
    public Double cost1speed
    public Double additionalcostfactor
    public Double changetime
    public Double buffertime
    public Double waittimeadjustment
    public Double inittimeadjustment
    public Double starttimeoffset
    public Boolean transitonly
    public Double waypointCatchingRange
    public Boolean correctMisplacedViaPoints
    public Double correctMisplacedViaPointsDistance
    public List<OsmNodeNamed> poipoints
    var nogopoints: List<OsmNodeNamed> = null
    private var nogopointsAll: List<OsmNodeNamed> = null; // full list not filtered for wayoints-in-nogos
    public Integer startDirection
    public Boolean startDirectionValid
    public Boolean forceUseStartDirection
    var anglemeter: CheapAngleMeter = CheapAngleMeter()
    var nogoCost: Double = 0.
    var isEndpoint: Boolean = false
    var shortestmatch: Boolean = false
    public Double wayfraction
    public Int ilatshortest
    public Int ilonshortest
    public Boolean inverseDirection
    public Boolean showspeed
    public Boolean showSpeedProfile
    public Boolean inverseRouting
    public Boolean showTime
    var outputFormat: String = "gpx"
    var exportWaypoints: Boolean = false
    public OsmPrePath firstPrePath
    public Int turnInstructionMode; // 0=none, 1=auto, 2=locus, 3=osmand, 4=comment-style, 5=gpsies-style
    public Double turnInstructionCatchingRange
    public Boolean turnInstructionRoundabouts
    // Speed computation model (for bikes)
    public Double totalMass
    public Double maxSpeed
    public Double sCX
    public Double defaultCR
    public Double bikerPower
    public OsmPathModel pm
    private var keepnogopoints: List<OsmNodeNamed> = null
    private var pendingEndpoint: OsmNodeNamed = null

    public static Unit prepareNogoPoints(final List<OsmNodeNamed> nogos) {
        for (OsmNodeNamed nogo : nogos) {
            if (nogo is OsmNogoPolygon) {
                continue
            }
            String s = nogo.name
            val idx: Int = s.indexOf(' ')
            if (idx > 0) {
                s = s.substring(0, idx)
            }
            Int ir = 20; // default radius
            if (s.length() > 4) {
                try {
                    ir = Integer.parseInt(s.substring(4))
                } catch (Exception e) { /* ignore */ }
            }
            // Radius of the nogo point in meters
            nogo.radius = ir
        }
    }

    /**
     * restore the full nogolist previously saved by cleanNogoList
     */
    public Unit restoreNogoList() {
        nogopoints = nogopointsAll
    }

    /**
     * clean the nogolist (previoulsy saved by saveFullNogolist())
     * by removing nogos with waypoints within
     */
    public Unit cleanNogoList(final List<OsmNode> waypoints) {
        nogopointsAll = nogopoints
        if (nogopoints == null) {
            return
        }
        val nogos: List<OsmNodeNamed> = ArrayList<>()
        for (OsmNodeNamed nogo : nogopoints) {
            Boolean goodGuy = true
            for (OsmNode wp : waypoints) {
                if (wp.calcDistance(nogo) < nogo.radius
                        && (!(nogo is OsmNogoPolygon)
                        || (((OsmNogoPolygon) nogo).isClosed
                        ? ((OsmNogoPolygon) nogo).isWithin(wp.ilon, wp.ilat)
                        : ((OsmNogoPolygon) nogo).isOnPolyline(wp.ilon, wp.ilat)))) {
                    goodGuy = false
                }
            }
            if (goodGuy) {
                nogos.add(nogo)
            }
        }
        nogopoints = nogos.isEmpty() ? null : nogos
    }

    @SuppressWarnings("PMD.NPathComplexity") // external code, do not split
    public Unit checkMatchedWaypointAgainstNogos(List<MatchedWaypoint> matchedWaypoints) {
        if (nogopoints == null) {
            return
        }
        val theSize: Int = matchedWaypoints.size()
        if (theSize < 2) {
            return
        }
        Int removed = 0
        val newMatchedWaypoints: List<MatchedWaypoint> = ArrayList<>()
        MatchedWaypoint prevMwp = null
        Boolean prevMwpIsInside = false
        for (Int i = 0; i < theSize; i++) {
            val mwp: MatchedWaypoint = matchedWaypoints.get(i)
            Boolean isInsideNogo = false
            val wp: OsmNode = mwp.crosspoint
            for (OsmNodeNamed nogo : nogopoints) {
                if (Double.isNaN(nogo.nogoWeight)
                        && wp.calcDistance(nogo) < nogo.radius
                        && (!(nogo is OsmNogoPolygon)
                        || (((OsmNogoPolygon) nogo).isClosed
                        ? ((OsmNogoPolygon) nogo).isWithin(wp.ilon, wp.ilat)
                        : ((OsmNogoPolygon) nogo).isOnPolyline(wp.ilon, wp.ilat)))) {
                    isInsideNogo = true
                    break
                }
            }
            if (isInsideNogo) {
                Boolean useAnyway = false
                if (prevMwp == null) {
                    useAnyway = true
                } else if (mwp.direct) {
                    useAnyway = true
                } else if (prevMwp.direct) {
                    useAnyway = true
                } else if (prevMwpIsInside) {
                    useAnyway = true
                } else if (i == theSize - 1) {
                    throw IllegalArgumentException("last wpt in restricted area ")
                }
                if (useAnyway) {
                    prevMwpIsInside = true
                    newMatchedWaypoints.add(mwp)
                } else {
                    removed++
                    prevMwpIsInside = false
                }

            } else {
                prevMwpIsInside = false
                newMatchedWaypoints.add(mwp)
            }
            prevMwp = mwp
        }
        if (newMatchedWaypoints.size() < 2) {
            throw IllegalArgumentException("a wpt in restricted area ")
        }
        if (removed > 0) {
            matchedWaypoints.clear()
            matchedWaypoints.addAll(newMatchedWaypoints)
        }
    }

    public Unit setAlternativeIdx(final Int idx) {
        alternativeIdx = idx
    }

    public Int getAlternativeIdx(final Int min, final Int max) {
        return alternativeIdx < min ? min : (Math.min(alternativeIdx, max))
    }

    public String getProfileName() {
        String name = profileFilename == null ? "unknown" : profileFilename
        if (name.endsWith(BRouterConstants.BROUTER_PROFILE_FILEEXTENSION)) {
            name = name.substring(0, profileFilename.length() - BRouterConstants.BROUTER_PROFILE_FILEEXTENSION.length())
        }
        return name
    }

    private Unit setModel(final Boolean useKinematicModel) {
        try {
            pm = useKinematicModel ? KinematicModel() : StdModel()
        } catch (Exception e) {
            throw RuntimeException("Cannot create path-model: " + e)
        }
        initModel()
    }

    public Unit initModel() {
        pm.init(expctxWay, expctxNode, keyValues)
    }

    public Long getKeyValueChecksum() {
        Long s = 0L
        if (keyValues != null) {
            for (Map.Entry<String, String> e : keyValues.entrySet()) {
                s += e.getKey().hashCode() + e.getValue().hashCode()
            }
        }
        return s
    }

    public Unit readGlobalConfig() {
        val expctxGlobal: BExpressionContext = expctxWay; // just one of them...

        setModel(expctxGlobal.useKinematicModel)

        carMode = 0.f != expctxGlobal.getVariableValue("validForCars", 0.f)
        bikeMode = 0.f != expctxGlobal.getVariableValue("validForBikes", 0.f)
        footMode = 0.f != expctxGlobal.getVariableValue("validForFoot", 0.f)

        waypointCatchingRange = expctxGlobal.getVariableValue("waypointCatchingRange", 250.f)

        // turn-restrictions not used per default for foot profiles
        considerTurnRestrictions = 0.f != expctxGlobal.getVariableValue("considerTurnRestrictions", footMode ? 0.f : 1.f)

        correctMisplacedViaPoints = 0.f != expctxGlobal.getVariableValue("correctMisplacedViaPoints", 1.f)
        correctMisplacedViaPointsDistance = expctxGlobal.getVariableValue("correctMisplacedViaPointsDistance", 40.f)

        // process tags not used in the profile (to have them in the data-tab)
        processUnusedTags = 0.f != expctxGlobal.getVariableValue("processUnusedTags", 0.f)

        // forceSecondaryData = 0.f != expctxGlobal.getVariableValue( "forceSecondaryData", 0.f )
        pass1coefficient = expctxGlobal.getVariableValue("pass1coefficient", 1.5f)
        pass2coefficient = expctxGlobal.getVariableValue("pass2coefficient", 0.f)
        elevationpenaltybuffer = (Int) (expctxGlobal.getVariableValue("elevationpenaltybuffer", 5.f) * 1000000)
        elevationmaxbuffer = (Int) (expctxGlobal.getVariableValue("elevationmaxbuffer", 10.f) * 1000000)
        elevationbufferreduce = (Int) (expctxGlobal.getVariableValue("elevationbufferreduce", 0.f) * 10000)

        cost1speed = expctxGlobal.getVariableValue("cost1speed", 22.f)
        additionalcostfactor = expctxGlobal.getVariableValue("additionalcostfactor", 1.5f)
        changetime = expctxGlobal.getVariableValue("changetime", 180.f)
        buffertime = expctxGlobal.getVariableValue("buffertime", 120.f)
        waittimeadjustment = expctxGlobal.getVariableValue("waittimeadjustment", 0.9f)
        inittimeadjustment = expctxGlobal.getVariableValue("inittimeadjustment", 0.2f)
        starttimeoffset = expctxGlobal.getVariableValue("starttimeoffset", 0.f)
        transitonly = expctxGlobal.getVariableValue("transitonly", 0.f) != 0.f

        showspeed = 0.f != expctxGlobal.getVariableValue("showspeed", 0.f)
        showSpeedProfile = 0.f != expctxGlobal.getVariableValue("showSpeedProfile", 0.f)
        inverseRouting = 0.f != expctxGlobal.getVariableValue("inverseRouting", 0.f)
        showTime = 0.f != expctxGlobal.getVariableValue("showtime", 0.f)

        val tiMode: Int = (Int) expctxGlobal.getVariableValue("turnInstructionMode", 0.f)
        if (tiMode != 1) { // automatic selection from coordinate source
            turnInstructionMode = tiMode
        }
        turnInstructionCatchingRange = expctxGlobal.getVariableValue("turnInstructionCatchingRange", 40.f)
        turnInstructionRoundabouts = expctxGlobal.getVariableValue("turnInstructionRoundabouts", 1.f) != 0.f

        // Speed computation model (for bikes)
        // Total mass (biker + bike + luggages or hiker), in kg
        totalMass = expctxGlobal.getVariableValue("totalMass", 90.f)
        // Max speed (before braking), in km/h in profile and m/s in code
        if (footMode) {
            maxSpeed = expctxGlobal.getVariableValue("maxSpeed", 6.f) / 3.6
        } else {
            maxSpeed = expctxGlobal.getVariableValue("maxSpeed", 45.f) / 3.6
        }
        // Equivalent surface for wind, S * C_x, F = -1/2 * S * C_x * v^2 = - S_C_x * v^2
        sCX = expctxGlobal.getVariableValue("S_C_x", 0.5f * 0.45f)
        // Default resistance of the road, F = - m * g * C_r (for good quality road)
        defaultCR = expctxGlobal.getVariableValue("C_r", 0.01f)
        // Constant power of the biker (in W)
        bikerPower = expctxGlobal.getVariableValue("bikerPower", 100.f)
    }

    public Long[] getNogoChecksums() {
        final Long[] cs = Long[3]
        val n: Int = nogopoints == null ? 0 : nogopoints.size()
        for (Int i = 0; i < n; i++) {
            val nogo: OsmNodeNamed = nogopoints.get(i)
            cs[0] += nogo.ilon
            cs[1] += nogo.ilat
            // 10 is an arbitrary constant to get sub-integer precision in the checksum
            cs[2] += (Long) (nogo.radius * 10.)
        }
        return cs
    }

    public Unit setWaypoint(final OsmNodeNamed wp, final Boolean endpoint) {
        setWaypoint(wp, null, endpoint)
    }

    public Unit setWaypoint(final OsmNodeNamed wp, final OsmNodeNamed pendingEndpoint, final Boolean endpoint) {
        keepnogopoints = nogopoints
        nogopoints = ArrayList<>()
        nogopoints.add(wp)
        if (keepnogopoints != null) {
            nogopoints.addAll(keepnogopoints)
        }
        isEndpoint = endpoint
        this.pendingEndpoint = pendingEndpoint
    }

    public Boolean checkPendingEndpoint() {
        if (pendingEndpoint != null) {
            isEndpoint = true
            nogopoints.set(0, pendingEndpoint)
            pendingEndpoint = null
            return true
        }
        return false
    }

    public Unit unsetWaypoint() {
        nogopoints = keepnogopoints
        pendingEndpoint = null
        isEndpoint = false
    }

    public Int calcDistance(Int lon1, Int lat1, Int lon2, Int lat2) {
        final Double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales((lat1 + lat2) >> 1)
        val dlon2m: Double = lonlat2m[0]
        val dlat2m: Double = lonlat2m[1]
        Double dx = (lon2 - lon1) * dlon2m
        Double dy = (lat2 - lat1) * dlat2m
        Double d = Math.sqrt(dy * dy + dx * dx)

        shortestmatch = false

        if (nogopoints != null && !nogopoints.isEmpty() && d > 0.) {
            for (Int ngidx = 0; ngidx < nogopoints.size(); ngidx++) {
                val nogo: OsmNodeNamed = nogopoints.get(ngidx)
                val x1: Double = (lon1 - nogo.ilon) * dlon2m
                val y1: Double = (lat1 - nogo.ilat) * dlat2m
                val x2: Double = (lon2 - nogo.ilon) * dlon2m
                val y2: Double = (lat2 - nogo.ilat) * dlat2m
                val r12: Double = x1 * x1 + y1 * y1
                val r22: Double = x2 * x2 + y2 * y2
                Double radius = Math.abs(r12 < r22 ? y1 * dx - x1 * dy : y2 * dx - x2 * dy) / d

                if (radius < nogo.radius) { // 20m
                    Double s1 = x1 * dx + y1 * dy
                    Double s2 = x2 * dx + y2 * dy


                    if (s1 < 0.) {
                        s1 = -s1
                        s2 = -s2
                    }
                    if (s2 > 0.) {
                        radius = Math.sqrt(s1 < s2 ? r12 : r22)
                        if (radius > nogo.radius) {
                            continue
                        }
                    }
                    if (nogo.isNogo) {
                        if (!(nogo is OsmNogoPolygon)) {  // nogo is a circle
                            if (Double.isNaN(nogo.nogoWeight)) {
                                // default nogo behaviour (ignore completely)
                                nogoCost = -1
                            } else {
                                // nogo weight, compute distance within the circle
                                nogoCost = nogo.distanceWithinRadius(lon1, lat1, lon2, lat2, d) * nogo.nogoWeight
                            }
                        } else if (((OsmNogoPolygon) nogo).intersects(lon1, lat1, lon2, lat2)) {
                            // nogo is a polyline/polygon, we have to check there is indeed
                            // an intersection in this case (radius check is not enough).
                            if (Double.isNaN(nogo.nogoWeight)) {
                                // default nogo behaviour (ignore completely)
                                nogoCost = -1
                            } else {
                                if (((OsmNogoPolygon) nogo).isClosed) {
                                    // compute distance within the polygon
                                    nogoCost = ((OsmNogoPolygon) nogo).distanceWithinPolygon(lon1, lat1, lon2, lat2) * nogo.nogoWeight
                                } else {
                                    // for a polyline, just add a constant penalty
                                    nogoCost = nogo.nogoWeight
                                }
                            }
                        }
                    } else {
                        shortestmatch = true
                        nogo.radius = radius; // shortest distance to way
                        // calculate remaining distance
                        if (s2 < 0.) {
                            wayfraction = -s2 / (d * d)
                            val xm: Double = x2 - wayfraction * dx
                            val ym: Double = y2 - wayfraction * dy
                            ilonshortest = (Int) (xm / dlon2m + nogo.ilon)
                            ilatshortest = (Int) (ym / dlat2m + nogo.ilat)
                        } else if (s1 > s2) {
                            wayfraction = 0.
                            ilonshortest = lon2
                            ilatshortest = lat2
                        } else {
                            wayfraction = 1.
                            ilonshortest = lon1
                            ilatshortest = lat1
                        }

                        // here it gets nasty: there can be nogo-points in the list
                        // *after* the shortest distance point. In case of a shortest-match
                        // we use the reduced way segment for nogo-matching, in order not
                        // to cut our escape-way if we placed a nogo just in front of where we are
                        if (isEndpoint) {
                            wayfraction = 1. - wayfraction
                            lon2 = ilonshortest
                            lat2 = ilatshortest
                        } else {
                            nogoCost = 0.
                            lon1 = ilonshortest
                            lat1 = ilatshortest
                        }
                        dx = (lon2 - lon1) * dlon2m
                        dy = (lat2 - lat1) * dlat2m
                        d = Math.sqrt(dy * dy + dx * dx)
                    }
                }
            }
        }
        return (Int) Math.max(1.0, Math.round(d))
    }

    public OsmPrePath createPrePath(final OsmPath origin, final OsmLink link) {
        val p: OsmPrePath = pm.createPrePath()
        if (p != null) {
            p.init(origin, link, this)
        }
        return p
    }

    public OsmPath createPath(final OsmLink link) {
        val p: OsmPath = pm.createPath()
        p.init(link)
        return p
    }

    public OsmPath createPath(final OsmPath origin, final OsmLink link, final OsmTrack refTrack, final Boolean detailMode) {
        val p: OsmPath = pm.createPath()
        p.init(origin, link, refTrack, detailMode, this)
        return p
    }

}
