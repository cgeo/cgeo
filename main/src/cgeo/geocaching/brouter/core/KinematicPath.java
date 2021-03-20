/**
 * The path-instance of the kinematic model
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.util.FastMath;


final class KinematicPath extends OsmPath {
    private double ekin; // kinetic energy (Joule)
    private double totalTime;  // travel time (seconds)
    private double totalEnergy; // total route energy (Joule)
    private float floatingAngleLeft; // sliding average left bend (degree)
    private float floatingAngleRight; // sliding average right bend (degree)

    @Override
    protected void init(OsmPath orig) {
        KinematicPath origin = (KinematicPath) orig;
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
    protected double processWaySection(RoutingContext rc, double dist, double delta_h, double elevation, double angle, double cosangle, boolean isStartpoint, int nsection, int lastpriorityclassifier) {
        KinematicModel km = (KinematicModel) rc.pm;

        double cost = 0.;
        double extraTime = 0.;

        if (isStartpoint) {
            // for forward direction, we start with target speed
            if (!rc.inverseDirection) {
                extraTime = 0.5 * (1. - cosangle) * 40.; // 40 seconds turn penalty
            }
        } else {
            double turnspeed = 999.; // just high

            if (km.turnAngleDecayTime != 0.) // process turn-angle slowdown
            {
                if (angle < 0)
                    floatingAngleLeft -= (float) angle;
                else
                    floatingAngleRight += (float) angle;
                float aa = Math.max(floatingAngleLeft, floatingAngleRight);

                double curveSpeed = aa > 10. ? 200. / aa : 20.;
                double distanceTime = dist / curveSpeed;
                double decayFactor = FastMath.exp(-distanceTime / km.turnAngleDecayTime);
                floatingAngleLeft = (float) (floatingAngleLeft * decayFactor);
                floatingAngleRight = (float) (floatingAngleRight * decayFactor);

                if (curveSpeed < 20.) {
                    turnspeed = curveSpeed;
                }
            }

            if (nsection == 0) // process slowdown by crossing geometry
            {
                double junctionspeed = 999.; // just high

                int classifiermask = (int) rc.expctxWay.getClassifierMask();

                // penalty for equal priority crossing
                boolean hasLeftWay = false;
                boolean hasRightWay = false;
                boolean hasResidential = false;
                for (OsmPrePath prePath = rc.firstPrePath; prePath != null; prePath = prePath.next) {
                    KinematicPrePath pp = (KinematicPrePath) prePath;

                    if (((pp.classifiermask ^ classifiermask) & 8) != 0) // exactly one is linktype
                    {
                        continue;
                    }

                    if ((pp.classifiermask & 32) != 0) // touching a residential?
                    {
                        hasResidential = true;
                    }

                    if (pp.priorityclassifier > priorityclassifier || pp.priorityclassifier == priorityclassifier && priorityclassifier < 20) {
                        double diff = pp.angle - angle;
                        if (diff < -40. && diff > -140.)
                            hasLeftWay = true;
                        if (diff > 40. && diff < 140.)
                            hasRightWay = true;
                    }
                }
                double residentialSpeed = 13.;

                if (hasLeftWay && junctionspeed > km.leftWaySpeed)
                    junctionspeed = km.leftWaySpeed;
                if (hasRightWay && junctionspeed > km.rightWaySpeed)
                    junctionspeed = km.rightWaySpeed;
                if (hasResidential && junctionspeed > residentialSpeed)
                    junctionspeed = residentialSpeed;

                if ((lastpriorityclassifier < 20) ^ (priorityclassifier < 20)) {
                    extraTime += 10.;
                    junctionspeed = 0; // full stop for entering or leaving road network
                }

                if (lastpriorityclassifier != priorityclassifier && (classifiermask & 8) != 0) {
                    extraTime += 2.; // two seconds for entering a link-type
                }
                turnspeed = turnspeed > junctionspeed ? junctionspeed : turnspeed;

                if (message != null) {
                    message.vnode0 = (int) (junctionspeed * 3.6 + 0.5);
                }
            }
            cutEkin(km.totalweight, turnspeed); // apply turnspeed
        }

        // linear temperature correction
        double tcorr = (20. - km.outside_temp) * 0.0035;

        // air_pressure down 1mb/8m
        double ecorr = 0.0001375 * (elevation - 100.);

        double f_air = km.f_air * (1. + tcorr - ecorr);

        double distanceCost = evolveDistance(km, dist, delta_h, f_air);

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


    protected double evolveDistance(KinematicModel km, double dist, double delta_h, double f_air) {
        // elevation force
        double fh = delta_h * km.totalweight * 9.81 / dist;

        double effectiveSpeedLimit = km.getEffectiveSpeedLimit();
        double emax = 0.5 * km.totalweight * effectiveSpeedLimit * effectiveSpeedLimit;
        if (emax <= 0.) {
            return -1.;
        }
        double vb = km.getBreakingSpeed(effectiveSpeedLimit);
        double elow = 0.5 * km.totalweight * vb * vb;

        double elapsedTime = 0.;
        double dissipatedEnergy = 0.;

        double v = Math.sqrt(2. * ekin / km.totalweight);
        double d = dist;
        while (d > 0.) {
            boolean slow = ekin < elow;
            boolean fast = ekin >= emax;
            double etarget = slow ? elow : emax;
            double f = km.f_roll + f_air * v * v + fh;
            double f_recup = Math.max(0., fast ? -f : (slow ? km.f_recup : 0) - fh); // additional recup for slow part
            f += f_recup;

            double delta_ekin;
            double timeStep;
            double x;
            if (fast) {
                x = d;
                delta_ekin = x * f;
                timeStep = x / v;
                ekin = etarget;
            } else {
                delta_ekin = etarget - ekin;
                double b = 2. * f_air / km.totalweight;
                double x0 = delta_ekin / f;
                double x0b = x0 * b;
                x = x0 * (1. - x0b * (0.5 + x0b * (0.333333333 - x0b * 0.25))); // = ln( delta_ekin*b/f + 1.) / b;
                double maxstep = Math.min(50., d);
                if (x >= maxstep) {
                    x = maxstep;
                    double xb = x * b;
                    delta_ekin = x * f * (1. + xb * (0.5 + xb * (0.166666667 + xb * 0.0416666667))); // = f/b* exp(xb-1)
                    ekin += delta_ekin;
                } else {
                    ekin = etarget;
                }
                double v2 = Math.sqrt(2. * ekin / km.totalweight);
                double a = f / km.totalweight; // TODO: average force?
                timeStep = (v2 - v) / a;
                v = v2;
            }
            d -= x;
            elapsedTime += timeStep;

            // dissipated energy does not contain elevation and efficient recup
            dissipatedEnergy += delta_ekin - x * (fh + f_recup * km.recup_efficiency);

            // correction: inefficient recup going into heating is half efficient
            double ieRecup = x * f_recup * (1. - km.recup_efficiency);
            double eaux = timeStep * km.p_standby;
            dissipatedEnergy -= Math.max(ieRecup, eaux) * 0.5;
        }

        dissipatedEnergy += elapsedTime * km.p_standby;

        totalTime += elapsedTime;
        totalEnergy += dissipatedEnergy + dist * fh;

        return (km.pw * elapsedTime + dissipatedEnergy) / km.cost0; // =cost
    }

    @Override
    protected double processTargetNode(RoutingContext rc) {
        KinematicModel km = (KinematicModel) rc.pm;

        // finally add node-costs for target node
        if (targetNode.nodeDescription != null) {
            rc.expctxNode.evaluate(false, targetNode.nodeDescription);
            float initialcost = rc.expctxNode.getInitialcost();
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

    private void cutEkin(double weight, double speed) {
        double e = 0.5 * weight * speed * speed;
        if (ekin > e)
            ekin = e;
    }


    @Override
    public int elevationCorrection(RoutingContext rc) {
        return 0;
    }

    @Override
    public boolean definitlyWorseThan(OsmPath path, RoutingContext rc) {
        KinematicPath p = (KinematicPath) path;

        int c = p.cost;
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
