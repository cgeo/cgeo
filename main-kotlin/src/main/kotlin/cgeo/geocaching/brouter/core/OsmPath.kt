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
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.mapaccess.OsmLink
import cgeo.geocaching.brouter.mapaccess.OsmLinkHolder
import cgeo.geocaching.brouter.mapaccess.OsmNode
import cgeo.geocaching.brouter.mapaccess.OsmTransferNode
import cgeo.geocaching.brouter.mapaccess.TurnRestriction
import cgeo.geocaching.brouter.util.CheapRulerHelper

abstract class OsmPath : OsmLinkHolder {
    private static val PATH_START_BIT: Int = 1
    private static val CAN_LEAVE_DESTINATION_BIT: Int = 2
    private static val IS_ON_DESTINATION_BIT: Int = 4
    private static val HAD_DESTINATION_START_BIT: Int = 8
    /**
     * The cost of that path (a modified distance)
     */
    var cost: Int = 0
    // the elevation assumed for that path can have a value
    // if the corresponding node has not
    public Short selev
    var airdistance: Int = 0; // distance to endpos
    public OsmPathElement originElement
    public OsmPathElement myElement
    var treedepth: Int = 0
    // the position of the waypoint just before
    // this path position (for angle calculation)
    public Int originLon
    public Int originLat
    public MessageData message
    protected OsmNode sourceNode
    protected OsmNode targetNode
    protected OsmLink link
    // the classifier of the segment just before this paths position
    protected Float lastClassifier
    protected Float lastInitialCost
    protected Int priorityclassifier
    protected var bitfield: Int = PATH_START_BIT
    private var nextForLink: OsmLinkHolder = null
    static Int seg = 1

    private Boolean getBit(final Int mask) {
        return (bitfield & mask) != 0
    }

    private Unit setBit(final Int mask, final Boolean bit) {
        if (getBit(mask) != bit) {
            bitfield ^= mask
        }
    }

    public Boolean didEnterDestinationArea() {
        return !getBit(HAD_DESTINATION_START_BIT) && getBit(IS_ON_DESTINATION_BIT)
    }

    public Unit init(final OsmLink link) {
        this.link = link
        targetNode = link.getTarget(null)
        selev = targetNode.getSElev()

        originLon = -1
        originLat = -1
    }

    public Unit init(final OsmPath origin, final OsmLink link, final OsmTrack refTrack, final Boolean detailMode, final RoutingContext rc) {
        if (origin.myElement == null) {
            origin.myElement = OsmPathElement.create(origin)
        }
        this.originElement = origin.myElement
        this.link = link
        this.sourceNode = origin.targetNode
        this.targetNode = link.getTarget(sourceNode)
        this.cost = origin.cost
        this.lastClassifier = origin.lastClassifier
        this.lastInitialCost = origin.lastInitialCost
        this.bitfield = origin.bitfield
        this.priorityclassifier = origin.priorityclassifier
        init(origin)
        addAddionalPenalty(refTrack, detailMode, origin, link, rc)
    }

    protected abstract Unit init(OsmPath orig)

    protected abstract Unit resetState()

    protected Unit addAddionalPenalty(final OsmTrack refTrack, final Boolean detailMode, final OsmPath origin, final OsmLink link, final RoutingContext rc) {
        final Byte[] description = link.descriptionBitmap
        if (description == null) { // could be a beeline path
            message = MessageData()
            if (message != null) {
                message.turnangle = 0
                message.time = (Float) 1
                message.energy = (Float) 0
                message.priorityclassifier = 0
                message.classifiermask = 0
                message.lon = targetNode.getILon()
                message.lat = targetNode.getILat()
                message.ele = Short.MIN_VALUE
                message.linkdist = sourceNode.calcDistance(targetNode)
                message.wayKeyValues = "direct_segment=" + seg
                seg++
            }
            return
        }

        val recordTransferNodes: Boolean = detailMode

        rc.nogoCost = 0.

        // extract the 3 positions of the first section
        Int lon0 = origin.originLon
        Int lat0 = origin.originLat

        Int lon1 = sourceNode.getILon()
        Int lat1 = sourceNode.getILat()
        Short ele1 = origin.selev

        Int linkdisttotal = 0

        message = detailMode ? MessageData() : null

        val isReverse: Boolean = link.isReverse(sourceNode)

        // evaluate the way tags
        rc.expctxWay.evaluate(rc.inverseDirection ^ isReverse, description)


        // calculate the costfactor inputs
        val costfactor: Float = rc.expctxWay.getCostfactor()
        val isTrafficBackbone: Boolean = cost == 0 && rc.expctxWay.getIsTrafficBackbone() > 0.f
        val lastpriorityclassifier: Int = priorityclassifier
        priorityclassifier = (Int) rc.expctxWay.getPriorityClassifier()

        // *** add initial cost if the classifier changed
        val newClassifier: Float = rc.expctxWay.getInitialClassifier()
        val newInitialCost: Float = rc.expctxWay.getInitialcost()
        val classifierDiff: Float = newClassifier - lastClassifier
        if (newClassifier != 0. && lastClassifier != 0. && (classifierDiff > 0.0005 || classifierDiff < -0.0005)) {
            val initialcost: Float = rc.inverseDirection ? lastInitialCost : newInitialCost
            if (initialcost >= 1000000.) {
                cost = -1
                return
            }

            val iicost: Int = (Int) initialcost
            if (message != null) {
                message.linkinitcost += iicost
            }
            cost += iicost
        }
        lastClassifier = newClassifier
        lastInitialCost = newInitialCost

        // *** destination logic: no destination access in between
        val classifiermask: Int = (Int) rc.expctxWay.getClassifierMask()
        val newDestination: Boolean = (classifiermask & 64) != 0
        val oldDestination: Boolean = getBit(IS_ON_DESTINATION_BIT)
        if (getBit(PATH_START_BIT)) {
            setBit(PATH_START_BIT, false)
            setBit(CAN_LEAVE_DESTINATION_BIT, newDestination)
            setBit(HAD_DESTINATION_START_BIT, newDestination)
        } else {
            if (oldDestination && !newDestination) {
                if (getBit(CAN_LEAVE_DESTINATION_BIT)) {
                    setBit(CAN_LEAVE_DESTINATION_BIT, false)
                } else {
                    cost = -1
                    return
                }
            }
        }
        setBit(IS_ON_DESTINATION_BIT, newDestination)


        OsmTransferNode transferNode = link.geometry == null ? null
                : rc.geometryDecoder.decodeGeometry(link.geometry, sourceNode, targetNode, isReverse)

        for (Int nsection = 0; ; nsection++) {

            originLon = lon1
            originLat = lat1

            final Int lon2
            final Int lat2
            Short ele2
            final Short originEle2

            if (transferNode == null) {
                lon2 = targetNode.ilon
                lat2 = targetNode.ilat
                originEle2 = targetNode.selev
            } else {
                lon2 = transferNode.ilon
                lat2 = transferNode.ilat
                originEle2 = transferNode.selev
            }
            ele2 = originEle2

            Boolean isStartpoint = lon0 == -1 && lat0 == -1

            // check turn restrictions (n detail mode (=final pass) no TR to not mess up voice hints)
            if (nsection == 0 && rc.considerTurnRestrictions && !detailMode && !isStartpoint && (rc.inverseDirection
                    ? TurnRestriction.isTurnForbidden(sourceNode.firstRestriction, lon2, lat2, lon0, lat0, rc.bikeMode || rc.footMode, rc.carMode)
                    : TurnRestriction.isTurnForbidden(sourceNode.firstRestriction, lon0, lat0, lon2, lat2, rc.bikeMode || rc.footMode, rc.carMode))) {
                cost = -1
                return
            }

            // if recording, MessageData for each section (needed for turn-instructions)
            if (message != null && message.wayKeyValues != null) {
                originElement.message = message
                message = MessageData()
            }

            Int dist = rc.calcDistance(lon1, lat1, lon2, lat2)

            Boolean stopAtEndpoint = false
            if (rc.shortestmatch) {
                if (rc.isEndpoint) {
                    stopAtEndpoint = true
                    ele2 = interpolateEle(ele1, ele2, rc.wayfraction)
                } else {
                    // we just start here, reset everything
                    cost = 0
                    resetState()
                    lon0 = -1; // reset turncost-pipe
                    lat0 = -1
                    isStartpoint = true

                    if (recordTransferNodes) {
                        if (rc.wayfraction > 0.) {
                            ele1 = interpolateEle(ele1, ele2, 1. - rc.wayfraction)
                            originElement = OsmPathElement.create(rc.ilonshortest, rc.ilatshortest, ele1, null)
                        } else {
                            originElement = null; // prevent duplicate point
                        }
                    }

                    if (rc.checkPendingEndpoint()) {
                        dist = rc.calcDistance(rc.ilonshortest, rc.ilatshortest, lon2, lat2)
                        if (rc.shortestmatch) {
                            stopAtEndpoint = true
                            ele2 = interpolateEle(ele1, ele2, rc.wayfraction)
                        }
                    }
                }
            }

            if (message != null) {
                message.linkdist += dist
            }
            linkdisttotal += dist

            // apply a start-direction if appropriate (by faking the origin position)
            if (isStartpoint) {
                if (rc.startDirectionValid) {
                    val dir: Double = rc.startDirection * CheapRulerHelper.DEG_TO_RAD
                    final Double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales((lon0 + lat1) >> 1)
                    lon0 = lon1 - (Int) (1000. * Math.sin(dir) / lonlat2m[0])
                    lat0 = lat1 - (Int) (1000. * Math.cos(dir) / lonlat2m[1])
                } else {
                    lon0 = lon1 - (lon2 - lon1)
                    lat0 = lat1 - (lat2 - lat1)
                }
            }
            val angle: Double = rc.anglemeter.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2)
            val cosangle: Double = rc.anglemeter.getCosAngle()

            // *** elevation stuff
            Double deltaH = 0.
            if (ele2 == Short.MIN_VALUE) {
                ele2 = ele1
            }
            if (ele1 != Short.MIN_VALUE) {
                deltaH = (ele2 - ele1) / 4.
                if (rc.inverseDirection) {
                    deltaH = -deltaH
                }
            }


            val elevation: Double = ele2 == Short.MIN_VALUE ? 100. : ele2 / 4.

            Double sectionCost = processWaySection(rc, dist, deltaH, elevation, angle, cosangle, isStartpoint, nsection, lastpriorityclassifier)
            if ((sectionCost < 0. || costfactor > 9998. && !detailMode) || sectionCost + cost >= 2000000000.) {
                cost = -1
                return
            }

            if (isTrafficBackbone) {
                sectionCost = 0.
            }

            cost += (Int) sectionCost

            // compute kinematic
            computeKinematic(rc, dist, deltaH, detailMode)

            if (message != null) {
                message.turnangle = (Float) angle
                message.time = (Float) getTotalTime()
                message.energy = (Float) getTotalEnergy()
                message.priorityclassifier = priorityclassifier
                message.classifiermask = classifiermask
                message.lon = lon2
                message.lat = lat2
                message.ele = originEle2
                message.wayKeyValues = rc.expctxWay.getKeyValueDescription(isReverse, description)
            }

            if (stopAtEndpoint) {
                if (recordTransferNodes) {
                    originElement = OsmPathElement.create(rc.ilonshortest, rc.ilatshortest, originEle2, originElement)
                    originElement.cost = cost
                    if (message != null) {
                        originElement.message = message
                    }
                }
                if (rc.nogoCost < 0) {
                    cost = -1
                } else {
                    cost += rc.nogoCost
                }
                return
            }

            if (transferNode == null) {
                // *** penalty for being part of the reference track
                if (refTrack != null && refTrack.containsNode(targetNode) && refTrack.containsNode(sourceNode)) {
                    cost += linkdisttotal
                }
                selev = ele2
                break
            }
            transferNode = transferNode.next

            if (recordTransferNodes) {
                originElement = OsmPathElement.create(lon2, lat2, originEle2, originElement)
                originElement.cost = cost
            }
            lon0 = lon1
            lat0 = lat1
            lon1 = lon2
            lat1 = lat2
            ele1 = ele2
        }

        // check for nogo-matches (after the *actual* start of segment)
        if (rc.nogoCost < 0) {
            cost = -1
            return
        } else {
            cost += rc.nogoCost
        }

        // add target-node costs
        val targetCost: Double = processTargetNode(rc)
        if (targetCost < 0. || targetCost + cost >= 2000000000.) {
            cost = -1
            return
        }
        cost += (Int) targetCost
    }


    public Short interpolateEle(final Short e1, final Short e2, final Double fraction) {
        if (e1 == Short.MIN_VALUE || e2 == Short.MIN_VALUE) {
            return Short.MIN_VALUE
        }
        return (Short) (e1 * (1. - fraction) + e2 * fraction)
    }

    protected abstract Double processWaySection(RoutingContext rc, Double dist, Double deltaH, Double elevation, Double angle, Double cosangle, Boolean isStartpoint, Int nsection, Int lastpriorityclassifier)

    protected abstract Double processTargetNode(RoutingContext rc)

    protected Unit computeKinematic(final RoutingContext rc, final Double dist, final Double deltaH, final Boolean detailMode) {
        // default: nothing to do
    }

    public abstract Int elevationCorrection()

    public abstract Boolean definitlyWorseThan(OsmPath p)

    public OsmNode getSourceNode() {
        return sourceNode
    }

    public OsmNode getTargetNode() {
        return targetNode
    }

    public OsmLink getLink() {
        return link
    }

    public OsmLinkHolder getNextForLink() {
        return nextForLink
    }

    public Unit setNextForLink(final OsmLinkHolder holder) {
        nextForLink = holder
    }

    public Double getTotalTime() {
        return 0.
    }

    public Double getTotalEnergy() {
        return 0.
    }
}
