/**
 * The path-instance of the kinematic model
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.util.FastMathUtils;


final class KinematicPath extends OsmPath {
    private double ekin; // kinetic energy (Joule)
    private double totalTime;  // travel time (seconds)
    private double totalEnergy; // total route energy (Joule)
    private float floatingAngleLeft; // sliding average left bend (degree)
    private float floatingAngleRight; // sliding average right bend (degree)

    @Override
    protected void init(final OsmPath orig) {
        final KinematicPath origin = (KinematicPath) orig;
        ekin = origin.ekin;
        totalTime = origin.totalTime;
        totalEnergy = origin.totalEnergy;
        floatingAngleLeft = origin.floatingAngleLeft;
        floatingAngleRight = origin.floatingAngleRight;
        priorityclassifier = origin.priorityclassifier;
    }

    @Override
    protected void resetState() {
        ekin = 0.;
        totalTime = 0.;
        totalEnergy = 0.;
        floatingAngleLeft = 0.f;
        floatingAngleRight = 0.f;
    }

    @Override
    protected double processWaySection(final RoutingContext rc, final double dist, final double deltaH, final double elevation, final double angle, final double cosangle, final boolean isStartpoint, final int nsection, final int lastpriorityclassifier) {
        final KinematicModel km = (KinematicModel) rc.pm;

        double cost = 0.;
        double extraTime = 0.;

        if (isStartpoint) {
            // for forward direction, we start with target speed
            if (!rc.inverseDirection) {
                extraTime = 0.5 * (1. - cosangle) * 40.; // 40 seconds turn penalty
            }
        } else {
            double turnspeed = 999.; // just high

            if (km.turnAngleDecayTime != 0.) { // process turn-angle slowdown
                if (angle < 0) {
                    floatingAngleLeft -= (float) angle;
                } else {
                    floatingAngleRight += (float) angle;
                }
                final float aa = Math.max(floatingAngleLeft, floatingAngleRight);

                final double curveSpeed = aa > 10. ? 200. / aa : 20.;
                final double distanceTime = dist / curveSpeed;
                final double decayFactor = FastMathUtils.exp(-distanceTime / km.turnAngleDecayTime);
                floatingAngleLeft = (float) (floatingAngleLeft * decayFactor);
                floatingAngleRight = (float) (floatingAngleRight * decayFactor);

                if (curveSpeed < 20.) {
                    turnspeed = curveSpeed;
                }
            }

            if (nsection == 0) { // process slowdown by crossing geometry
                double junctionspeed = 999.; // just high

                final int classifiermask = (int) rc.expctxWay.getClassifierMask();

                // penalty for equal priority crossing
                boolean hasLeftWay = false;
                boolean hasRightWay = false;
                boolean hasResidential = false;
                for (OsmPrePath prePath = rc.firstPrePath; prePath != null; prePath = prePath.next) {
                    final KinematicPrePath pp = (KinematicPrePath) prePath;

                    if (((pp.classifiermask ^ classifiermask) & 8) != 0) { // exactly one is linktype
                        continue;
                    }

                    if ((pp.classifiermask & 32) != 0) { // touching a residential?
                        hasResidential = true;
                    }

                    if (pp.priorityclassifier > priorityclassifier || pp.priorityclassifier == priorityclassifier && priorityclassifier < 20) {
                        final double diff = pp.angle - angle;
                        if (diff < -40. && diff > -140.) {
                            hasLeftWay = true;
                        }
                        if (diff > 40. && diff < 140.) {
                            hasRightWay = true;
                        }
                    }
                }
                final double residentialSpeed = 13.;

                if (hasLeftWay && junctionspeed > km.leftWaySpeed) {
                    junctionspeed = km.leftWaySpeed;
                }
                if (hasRightWay && junctionspeed > km.rightWaySpeed) {
                    junctionspeed = km.rightWaySpeed;
                }
                if (hasResidential && junctionspeed > residentialSpeed) {
                    junctionspeed = residentialSpeed;
                }

                if ((lastpriorityclassifier < 20) ^ (priorityclassifier < 20)) {
                    extraTime += 10.;
                    junctionspeed = 0; // full stop for entering or leaving road network
                }

                if (lastpriorityclassifier != priorityclassifier && (classifiermask & 8) != 0) {
                    extraTime += 2.; // two seconds for entering a link-type
                }
                turnspeed = Math.min(turnspeed, junctionspeed);

                if (message != null) {
                    message.vnode0 = (int) (junctionspeed * 3.6 + 0.5);
                }
            }
            cutEkin(km.totalweight, turnspeed); // apply turnspeed
        }

        // linear temperature correction
        final double tcorr = (20. - km.outsideTemp) * 0.0035;

        // air_pressure down 1mb/8m
        final double ecorr = 0.0001375 * (elevation - 100.);

        final double fAir = km.fAir * (1. + tcorr - ecorr);

        final double distanceCost = evolveDistance(km, dist, deltaH, fAir);

        if (message != null) {
            message.costfactor = (float) (distanceCost / dist);
            message.vmax = (int) (km.getWayMaxspeed() * 3.6 + 0.5);
            message.vmaxExplicit = (int) (km.getWayMaxspeedExplicit() * 3.6 + 0.5);
            message.vmin = (int) (km.getWayMinspeed() * 3.6 + 0.5);
            message.extraTime = (int) (extraTime * 1000);
        }

        cost += extraTime * km.pw / km.cost0;
        totalTime += extraTime;

        return cost + distanceCost;
    }


    protected double evolveDistance(final KinematicModel km, final double dist, final double deltaH, final double fAir) {
        // elevation force
        final double fh = deltaH * km.totalweight * 9.81 / dist;

        final double effectiveSpeedLimit = km.getEffectiveSpeedLimit();
        final double emax = 0.5 * km.totalweight * effectiveSpeedLimit * effectiveSpeedLimit;
        if (emax <= 0.) {
            return -1.;
        }
        final double vb = km.getBreakingSpeed(effectiveSpeedLimit);
        final double elow = 0.5 * km.totalweight * vb * vb;

        double elapsedTime = 0.;
        double dissipatedEnergy = 0.;

        double v = Math.sqrt(2. * ekin / km.totalweight);
        double d = dist;
        while (d > 0.) {
            final boolean slow = ekin < elow;
            final boolean fast = ekin >= emax;
            final double etarget = slow ? elow : emax;
            double f = km.fRoll + fAir * v * v + fh;
            final double fRecup = Math.max(0., fast ? -f : (slow ? km.fRecup : 0) - fh); // additional recup for slow part
            f += fRecup;

            double deltaEkin;
            final double timeStep;
            double x;
            if (fast) {
                x = d;
                deltaEkin = x * f;
                timeStep = x / v;
                ekin = etarget;
            } else {
                deltaEkin = etarget - ekin;
                final double b = 2. * fAir / km.totalweight;
                final double x0 = deltaEkin / f;
                final double x0b = x0 * b;
                x = x0 * (1. - x0b * (0.5 + x0b * (0.333333333 - x0b * 0.25))); // = ln( deltaEkin*b/f + 1.) / b;
                final double maxstep = Math.min(50., d);
                if (x >= maxstep) {
                    x = maxstep;
                    final double xb = x * b;
                    deltaEkin = x * f * (1. + xb * (0.5 + xb * (0.166666667 + xb * 0.0416666667))); // = f/b* exp(xb-1)
                    ekin += deltaEkin;
                } else {
                    ekin = etarget;
                }
                final double v2 = Math.sqrt(2. * ekin / km.totalweight);
                final double a = f / km.totalweight; // TODO: average force?
                timeStep = (v2 - v) / a;
                v = v2;
            }
            d -= x;
            elapsedTime += timeStep;

            // dissipated energy does not contain elevation and efficient recup
            dissipatedEnergy += deltaEkin - x * (fh + fRecup * km.recupEfficiency);

            // correction: inefficient recup going into heating is half efficient
            final double ieRecup = x * fRecup * (1. - km.recupEfficiency);
            final double eaux = timeStep * km.pStandby;
            dissipatedEnergy -= Math.max(ieRecup, eaux) * 0.5;
        }

        dissipatedEnergy += elapsedTime * km.pStandby;

        totalTime += elapsedTime;
        totalEnergy += dissipatedEnergy + dist * fh;

        return (km.pw * elapsedTime + dissipatedEnergy) / km.cost0; // =cost
    }

    @Override
    protected double processTargetNode(final RoutingContext rc) {
        final KinematicModel km = (KinematicModel) rc.pm;

        // finally add node-costs for target node
        if (targetNode.nodeDescription != null) {
            rc.expctxNode.evaluate(false, targetNode.nodeDescription);
            final float initialcost = rc.expctxNode.getInitialcost();
            if (initialcost >= 1000000.) {
                return -1.;
            }
            cutEkin(km.totalweight, km.getNodeMaxspeed()); // apply node maxspeed

            if (message != null) {
                message.linknodecost += (int) initialcost;
                message.nodeKeyValues = rc.expctxNode.getKeyValueDescription(false, targetNode.nodeDescription);

                message.vnode1 = (int) (km.getNodeMaxspeed() * 3.6 + 0.5);
            }
            return initialcost;
        }
        return 0.;
    }

    private void cutEkin(final double weight, final double speed) {
        final double e = 0.5 * weight * speed * speed;
        if (ekin > e) {
            ekin = e;
        }
    }


    @Override
    public int elevationCorrection(final RoutingContext rc) {
        return 0;
    }

    @Override
    public boolean definitlyWorseThan(final OsmPath path, final RoutingContext rc) {
        final KinematicPath p = (KinematicPath) path;

        final int c = p.cost;
        return cost > c + 100;
    }

    @Override
    public double getTotalTime() {
        return totalTime;
    }

    @Override
    public double getTotalEnergy() {
        return totalEnergy;
    }
}
