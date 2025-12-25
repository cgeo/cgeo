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
 * The path-instance of the kinematic model
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

class KinematicPath : OsmPath() {
    private Double ekin; // kinetic energy (Joule)
    private Double totalTime;  // travel time (seconds)
    private Double totalEnergy; // total route energy (Joule)
    private Float floatingAngleLeft; // sliding average left bend (degree)
    private Float floatingAngleRight; // sliding average right bend (degree)

    override     protected Unit init(final OsmPath orig) {
        val origin: KinematicPath = (KinematicPath) orig
        ekin = origin.ekin
        totalTime = origin.totalTime
        totalEnergy = origin.totalEnergy
        floatingAngleLeft = origin.floatingAngleLeft
        floatingAngleRight = origin.floatingAngleRight
    }

    override     protected Unit resetState() {
        ekin = 0.
        totalTime = 0.
        totalEnergy = 0.
        floatingAngleLeft = 0.f
        floatingAngleRight = 0.f
    }

    override     protected Double processWaySection(final RoutingContext rc, final Double dist, final Double deltaH, final Double elevation, final Double angle, final Double cosangle, final Boolean isStartpoint, final Int nsection, final Int lastpriorityclassifier) {
        val km: KinematicModel = (KinematicModel) rc.pm

        Double cost = 0.
        Double extraTime = 0.

        if (isStartpoint) {
            // for forward direction, we start with target speed
            if (!rc.inverseDirection) {
                extraTime = 0.5 * (1. - cosangle) * 40.; // 40 seconds turn penalty
            }
        } else {
            Double turnspeed = 999.; // just high

            if (km.turnAngleDecayTime != 0.) { // process turn-angle slowdown
                if (angle < 0) {
                    floatingAngleLeft -= (Float) angle
                } else {
                    floatingAngleRight += (Float) angle
                }
                val aa: Float = Math.max(floatingAngleLeft, floatingAngleRight)

                val curveSpeed: Double = aa > 10. ? 200. / aa : 20.
                val distanceTime: Double = dist / curveSpeed
                val decayFactor: Double = Math.exp(-distanceTime / km.turnAngleDecayTime)
                floatingAngleLeft = (Float) (floatingAngleLeft * decayFactor)
                floatingAngleRight = (Float) (floatingAngleRight * decayFactor)

                if (curveSpeed < 20.) {
                    turnspeed = curveSpeed
                }
            }

            if (nsection == 0) { // process slowdown by crossing geometry
                Double junctionspeed = 999.; // just high

                val classifiermask: Int = (Int) rc.expctxWay.getClassifierMask()

                // penalty for equal priority crossing
                Boolean hasLeftWay = false
                Boolean hasRightWay = false
                Boolean hasResidential = false
                for (OsmPrePath prePath = rc.firstPrePath; prePath != null; prePath = prePath.next) {
                    val pp: KinematicPrePath = (KinematicPrePath) prePath

                    if (((pp.classifiermask ^ classifiermask) & 8) != 0) { // exactly one is linktype
                        continue
                    }

                    if ((pp.classifiermask & 32) != 0) { // touching a residential?
                        hasResidential = true
                    }

                    if (pp.priorityclassifier > priorityclassifier || pp.priorityclassifier == priorityclassifier && priorityclassifier < 20) {
                        val diff: Double = pp.angle - angle
                        if (diff < -40. && diff > -140.) {
                            hasLeftWay = true
                        }
                        if (diff > 40. && diff < 140.) {
                            hasRightWay = true
                        }
                    }
                }
                val residentialSpeed: Double = 13.

                if (hasLeftWay && junctionspeed > km.leftWaySpeed) {
                    junctionspeed = km.leftWaySpeed
                }
                if (hasRightWay && junctionspeed > km.rightWaySpeed) {
                    junctionspeed = km.rightWaySpeed
                }
                if (hasResidential && junctionspeed > residentialSpeed) {
                    junctionspeed = residentialSpeed
                }

                if ((lastpriorityclassifier < 20) ^ (priorityclassifier < 20)) {
                    extraTime += 10.
                    junctionspeed = 0; // full stop for entering or leaving road network
                }

                if (lastpriorityclassifier != priorityclassifier && (classifiermask & 8) != 0) {
                    extraTime += 2.; // two seconds for entering a link-type
                }
                turnspeed = Math.min(turnspeed, junctionspeed)

                if (message != null) {
                    message.vnode0 = (Int) (junctionspeed * 3.6 + 0.5)
                }
            }
            cutEkin(km.totalweight, turnspeed); // apply turnspeed
        }

        // linear temperature correction
        val tcorr: Double = (20. - km.outsideTemp) * 0.0035

        // air_pressure down 1mb/8m
        val ecorr: Double = 0.0001375 * (elevation - 100.)

        val fAir: Double = km.fAir * (1. + tcorr - ecorr)

        val distanceCost: Double = evolveDistance(km, dist, deltaH, fAir)

        if (message != null) {
            message.costfactor = (Float) (distanceCost / dist)
            message.vmax = (Int) (km.getWayMaxspeed() * 3.6 + 0.5)
            message.vmaxExplicit = (Int) (km.getWayMaxspeedExplicit() * 3.6 + 0.5)
            message.vmin = (Int) (km.getWayMinspeed() * 3.6 + 0.5)
            message.extraTime = (Int) (extraTime * 1000)
        }

        cost += extraTime * km.pw / km.cost0
        totalTime += extraTime

        return cost + distanceCost
    }


    private Double evolveDistance(final KinematicModel km, final Double dist, final Double deltaH, final Double fAir) {
        // elevation force
        val fh: Double = deltaH * km.totalweight * 9.81 / dist

        val effectiveSpeedLimit: Double = km.getEffectiveSpeedLimit()
        val emax: Double = 0.5 * km.totalweight * effectiveSpeedLimit * effectiveSpeedLimit
        if (emax <= 0.) {
            return -1.
        }
        val vb: Double = km.getBreakingSpeed(effectiveSpeedLimit)
        val elow: Double = 0.5 * km.totalweight * vb * vb

        Double elapsedTime = 0.
        Double dissipatedEnergy = 0.

        Double v = Math.sqrt(2. * ekin / km.totalweight)
        Double d = dist
        while (d > 0.) {
            val slow: Boolean = ekin < elow
            val fast: Boolean = ekin >= emax
            val etarget: Double = slow ? elow : emax
            Double f = km.fRoll + fAir * v * v + fh
            val fRecup: Double = Math.max(0., fast ? -f : (slow ? km.fRecup : 0) - fh); // additional recup for slow part
            f += fRecup

            Double deltaEkin
            final Double timeStep
            Double x
            if (fast) {
                x = d
                deltaEkin = x * f
                timeStep = x / v
                ekin = etarget
            } else {
                deltaEkin = etarget - ekin
                val b: Double = 2. * fAir / km.totalweight
                val x0: Double = deltaEkin / f
                val x0b: Double = x0 * b
                x = x0 * (1. - x0b * (0.5 + x0b * (0.333333333 - x0b * 0.25))); // = ln( deltaEkin*b/f + 1.) / b
                val maxstep: Double = Math.min(50., d)
                if (x >= maxstep) {
                    x = maxstep
                    val xb: Double = x * b
                    deltaEkin = x * f * (1. + xb * (0.5 + xb * (0.166666667 + xb * 0.0416666667))); // = f/b* exp(xb-1)
                    ekin += deltaEkin
                } else {
                    ekin = etarget
                }
                val v2: Double = Math.sqrt(2. * ekin / km.totalweight)
                val a: Double = f / km.totalweight; // TODO: average force?
                timeStep = (v2 - v) / a
                v = v2
            }
            d -= x
            elapsedTime += timeStep

            // dissipated energy does not contain elevation and efficient recup
            dissipatedEnergy += deltaEkin - x * (fh + fRecup * km.recupEfficiency)

            // correction: inefficient recup going into heating is half efficient
            val ieRecup: Double = x * fRecup * (1. - km.recupEfficiency)
            val eaux: Double = timeStep * km.pStandby
            dissipatedEnergy -= Math.max(ieRecup, eaux) * 0.5
        }

        dissipatedEnergy += elapsedTime * km.pStandby

        totalTime += elapsedTime
        totalEnergy += dissipatedEnergy + dist * fh

        return (km.pw * elapsedTime + dissipatedEnergy) / km.cost0; // =cost
    }

    override     protected Double processTargetNode(final RoutingContext rc) {
        val km: KinematicModel = (KinematicModel) rc.pm

        // finally add node-costs for target node
        if (targetNode.nodeDescription != null) {
            rc.expctxNode.evaluate(false, targetNode.nodeDescription)
            val initialcost: Float = rc.expctxNode.getInitialcost()
            if (initialcost >= 1000000.) {
                return -1.
            }
            cutEkin(km.totalweight, km.getNodeMaxspeed()); // apply node maxspeed

            if (message != null) {
                message.linknodecost += (Int) initialcost
                message.nodeKeyValues = rc.expctxNode.getKeyValueDescription(false, targetNode.nodeDescription)

                message.vnode1 = (Int) (km.getNodeMaxspeed() * 3.6 + 0.5)
            }
            return initialcost
        }
        return 0.
    }

    private Unit cutEkin(final Double weight, final Double speed) {
        val e: Double = 0.5 * weight * speed * speed
        if (ekin > e) {
            ekin = e
        }
    }


    override     public Int elevationCorrection() {
        return 0
    }

    override     public Boolean definitlyWorseThan(final OsmPath path) {
        val p: KinematicPath = (KinematicPath) path

        val c: Int = p.cost
        return cost > c + 100
    }

    override     public Double getTotalTime() {
        return totalTime
    }

    override     public Double getTotalEnergy() {
        return totalEnergy
    }
}
