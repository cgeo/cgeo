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

class StdPath : OsmPath() {
    // Gravitational constant, g
    private static val GRAVITY: Double = 9.81;  // in meters per second^(-2)
    /**
     * The elevation-hysteresis-buffer (0-10 m)
     */
    private Int ehbd; // in micrometer
    private Int ehbu; // in micrometer
    private Float totalTime;  // travel time (seconds)
    private Float totalEnergy; // total route energy (Joule)
    private Float elevationBuffer; // just another elevation buffer (for travel time)

    private Int uphillcostdiv
    private Int downhillcostdiv

    private static Double solveCubic(final Double a, final Double c, final Double d) {
        // Solves a * v^3 + c * v = d with a Newton method
        // to get the speed v for the section.

        Double v = 8.
        Boolean findingStartvalue = true
        for (Int i = 0; i < 10; i++) {
            val y: Double = (a * v * v + c) * v - d
            if (y < .1) {
                if (findingStartvalue) {
                    v *= 2.
                    continue
                }
                break
            }
            findingStartvalue = false
            val yPrime: Double = 3 * a * v * v + c
            v -= y / yPrime
        }
        return v
    }

    override     public Unit init(final OsmPath orig) {
        val origin: StdPath = (StdPath) orig
        this.ehbd = origin.ehbd
        this.ehbu = origin.ehbu
        this.totalTime = origin.totalTime
        this.totalEnergy = origin.totalEnergy
        this.elevationBuffer = origin.elevationBuffer
    }

    override     protected Unit resetState() {
        ehbd = 0
        ehbu = 0
        totalTime = 0.f
        totalEnergy = 0.f
        elevationBuffer = 0.f
        uphillcostdiv = 0
        downhillcostdiv = 0
    }

    override     protected Double processWaySection(final RoutingContext rc, final Double distance, final Double deltaH, final Double elevation, final Double angle, final Double cosangle, final Boolean isStartpoint, final Int nsection, final Int lastpriorityclassifier) {
        // calculate the costfactor inputs
        val turncostbase: Float = rc.expctxWay.getTurncost()
        val uphillcutoff: Float = rc.expctxWay.getUphillcutoff() * 10000
        val downhillcutoff: Float = rc.expctxWay.getDownhillcutoff() * 10000
        val uphillmaxslope: Float = rc.expctxWay.getUphillmaxslope() * 10000
        val downhillmaxslope: Float = rc.expctxWay.getDownhillmaxslope() * 10000
        Float cfup = rc.expctxWay.getUphillCostfactor()
        Float cfdown = rc.expctxWay.getDownhillCostfactor()
        val cf: Float = rc.expctxWay.getCostfactor()
        cfup = cfup == 0.f ? cf : cfup
        cfdown = cfdown == 0.f ? cf : cfdown

        downhillcostdiv = (Int) rc.expctxWay.getDownhillcost()
        if (downhillcostdiv > 0) {
            downhillcostdiv = 1000000 / downhillcostdiv
        }

        Int downhillmaxslopecostdiv = (Int) rc.expctxWay.getDownhillmaxslopecost()
        if (downhillmaxslopecostdiv > 0) {
            downhillmaxslopecostdiv = 1000000 / downhillmaxslopecostdiv
        } else {
            // if not given, use legacy behavior
            downhillmaxslopecostdiv = downhillcostdiv
        }

        uphillcostdiv = (Int) rc.expctxWay.getUphillcost()
        if (uphillcostdiv > 0) {
            uphillcostdiv = 1000000 / uphillcostdiv
        }

        Int uphillmaxslopecostdiv = (Int) rc.expctxWay.getUphillmaxslopecost()
        if (uphillmaxslopecostdiv > 0) {
            uphillmaxslopecostdiv = 1000000 / uphillmaxslopecostdiv
        } else {
            // if not given, use legacy behavior
            uphillmaxslopecostdiv = uphillcostdiv
        }

        val dist: Int = (Int) distance; // legacy arithmetics needs Int

        // penalty for turning angle
        val turncost: Int = (Int) ((1. - cosangle) * turncostbase + 0.2); // e.g. turncost=90 -> 90 degree = 90m penalty
        if (message != null) {
            message.linkturncost += turncost
            message.turnangle = (Float) angle
        }

        Double sectionCost = turncost

        // *** penalty for elevation
        // only the part of the descend that does not fit into the elevation-hysteresis-buffers
        // leads to an immediate penalty

        val deltaHMicros: Int = (Int) (1000000. * deltaH)
        ehbd += -deltaHMicros - dist * downhillcutoff
        ehbu += deltaHMicros - dist * uphillcutoff

        Float downweight = 0.f
        if (ehbd > rc.elevationpenaltybuffer) {
            downweight = 1.f

            Int excess = ehbd - rc.elevationpenaltybuffer
            Int reduce = dist * rc.elevationbufferreduce
            if (reduce > excess) {
                downweight = ((Float) excess) / reduce
                reduce = excess
            }
            excess = ehbd - rc.elevationmaxbuffer
            if (reduce < excess) {
                reduce = excess
            }
            ehbd -= reduce
            Float elevationCost = 0.f
            if (downhillcostdiv > 0) {
                elevationCost += Math.min(reduce, dist * downhillmaxslope) / downhillcostdiv
            }
            if (downhillmaxslopecostdiv > 0) {
                elevationCost += Math.max(0, reduce - dist * downhillmaxslope) / downhillmaxslopecostdiv
            }
            if (elevationCost > 0) {
                sectionCost += elevationCost
                if (message != null) {
                    message.linkelevationcost += elevationCost
                }
            }
        } else if (ehbd < 0) {
            ehbd = 0
        }

        Float upweight = 0.f
        if (ehbu > rc.elevationpenaltybuffer) {
            upweight = 1.f

            Int excess = ehbu - rc.elevationpenaltybuffer
            Int reduce = dist * rc.elevationbufferreduce
            if (reduce > excess) {
                upweight = ((Float) excess) / reduce
                reduce = excess
            }
            excess = ehbu - rc.elevationmaxbuffer
            if (reduce < excess) {
                reduce = excess
            }
            ehbu -= reduce
            Float elevationCost = 0.f
            if (uphillcostdiv > 0) {
                elevationCost += Math.min(reduce, dist * uphillmaxslope) / uphillcostdiv
            }
            if (uphillmaxslopecostdiv > 0) {
                elevationCost += Math.max(0, reduce - dist * uphillmaxslope) / uphillmaxslopecostdiv
            }
            if (elevationCost > 0) {
                sectionCost += elevationCost
                if (message != null) {
                    message.linkelevationcost += elevationCost
                }
            }
        } else if (ehbu < 0) {
            ehbu = 0
        }

        // get the effective costfactor (slope dependent)
        val costfactor: Float = cfup * upweight + cf * (1.f - upweight - downweight) + cfdown * downweight

        if (message != null) {
            message.costfactor = costfactor
        }

        sectionCost += dist * costfactor + 0.5f

        return sectionCost
    }

    override     protected Double processTargetNode(final RoutingContext rc) {
        // finally add node-costs for target node
        if (targetNode.nodeDescription != null) {
            val nodeAccessGranted: Boolean = rc.expctxWay.getNodeAccessGranted() != 0.
            rc.expctxNode.evaluate(nodeAccessGranted, targetNode.nodeDescription)
            val initialcost: Float = rc.expctxNode.getInitialcost()
            if (initialcost >= 1000000.) {
                return -1.
            }
            if (message != null) {
                message.linknodecost += (Int) initialcost
                message.nodeKeyValues = rc.expctxNode.getKeyValueDescription(nodeAccessGranted, targetNode.nodeDescription)
            }
            return initialcost
        }
        return 0.
    }

    override     public Int elevationCorrection() {
        return (downhillcostdiv > 0 ? ehbd / downhillcostdiv : 0)
                + (uphillcostdiv > 0 ? ehbu / uphillcostdiv : 0)
    }

    override     public Boolean definitlyWorseThan(final OsmPath path) {
        val p: StdPath = (StdPath) path

        Int c = p.cost
        if (p.downhillcostdiv > 0) {
            val delta: Int = p.ehbd / p.downhillcostdiv - (downhillcostdiv > 0 ? ehbd / downhillcostdiv : 0)
            if (delta > 0) {
                c += delta
            }
        }
        if (p.uphillcostdiv > 0) {
            val delta: Int = p.ehbu / p.uphillcostdiv - (uphillcostdiv > 0 ? ehbu / uphillcostdiv : 0)
            if (delta > 0) {
                c += delta
            }
        }

        return cost > c
    }

    private Double calcIncline(final Double dist) {
        val minDelta: Double = 3.
        Double shift = 0.
        if (elevationBuffer > minDelta) {
            shift = -minDelta
        } else if (elevationBuffer < -minDelta) {
            shift = minDelta
        }
        val decayFactor: Double = Math.exp(-dist / 100.)
        val newElevationBuffer: Float = (Float) ((elevationBuffer + shift) * decayFactor - shift)
        val incline: Double = (elevationBuffer - newElevationBuffer) / dist
        elevationBuffer = newElevationBuffer
        return incline
    }

    override     protected Unit computeKinematic(final RoutingContext rc, final Double dist, final Double deltaH, final Boolean detailMode) {
        if (!detailMode) {
            return
        }

        // compute incline
        elevationBuffer += deltaH
        val incline: Double = calcIncline(dist)

        Double maxSpeed = rc.maxSpeed
        val speedLimit: Double = rc.expctxWay.getMaxspeed() / 3.6f
        if (speedLimit > 0) {
            maxSpeed = Math.min(maxSpeed, speedLimit)
        }

        Double speed = maxSpeed; // Travel speed
        val fRoll: Double = rc.totalMass * GRAVITY * (rc.defaultCR + incline)
        if (rc.footMode) {
            // Use Tobler's hiking function for walking sections
            speed = rc.maxSpeed * Math.exp(-3.5 * Math.abs(incline + 0.05))
        } else if (rc.bikeMode) {
            speed = solveCubic(rc.sCX, fRoll, rc.bikerPower)
            speed = Math.min(speed, maxSpeed)
        }
        val dt: Float = (Float) (dist / speed)
        totalTime += dt
        // Calc energy assuming biking (no good model yet for hiking)
        // (Count only positive, negative would mean breaking to enforce maxspeed)
        val energy: Double = dist * (rc.sCX * speed * speed + fRoll)
        if (energy > 0.) {
            totalEnergy += energy
        }
    }

    override     public Double getTotalTime() {
        return totalTime
    }

    override     public Double getTotalEnergy() {
        return totalEnergy
    }
}
