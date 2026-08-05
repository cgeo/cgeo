/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

final class StdPath extends OsmPath {
    // Gravitational constant, g
    private static final double GRAVITY = 9.81;  // in meters per second^(-2)
    /**
     * The elevation-hysteresis-buffer (0-10 m)
     */
    private int ehbd; // in micrometer
    private int ehbu; // in micrometer
    private float totalTime;  // travel time (seconds)
    private float totalEnergy; // total route energy (Joule)
    private float elevationBuffer; // just another elevation buffer (for travel time)

    private int uphillcostdiv;
    private int downhillcostdiv;

    private static double solveCubic(final double a, final double c, final double d) {
        // Solves a * v^3 + c * v = d with a Newton method
        // to get the speed v for the section.

        double v = 8.;
        boolean findingStartvalue = true;
        for (int i = 0; i < 10; i++) {
            final double y = (a * v * v + c) * v - d;
            if (y < .1) {
                if (findingStartvalue) {
                    v *= 2.;
                    continue;
                }
                break;
            }
            findingStartvalue = false;
            final double yPrime = 3 * a * v * v + c;
            v -= y / yPrime;
        }
        return v;
    }

    @Override
    public void init(final OsmPath orig) {
        final StdPath origin = (StdPath) orig;
        this.ehbd = origin.ehbd;
        this.ehbu = origin.ehbu;
        this.totalTime = origin.totalTime;
        this.totalEnergy = origin.totalEnergy;
        this.elevationBuffer = origin.elevationBuffer;
    }

    @Override
    protected void resetState() {
        ehbd = 0;
        ehbu = 0;
        totalTime = 0.f;
        totalEnergy = 0.f;
        elevationBuffer = 0.f;
        uphillcostdiv = 0;
        downhillcostdiv = 0;
    }

    @Override
    protected double processWaySection(final RoutingContext rc, final double distance, final double deltaH, final double elevation, final double angle, final double cosangle, final boolean isStartpoint, final int nsection, final int lastpriorityclassifier) {
        // calculate the costfactor inputs
        final float turncostbase = rc.expctxWay.getTurncost();
        final float uphillcutoff = rc.expctxWay.getUphillcutoff() * 10000;
        final float downhillcutoff = rc.expctxWay.getDownhillcutoff() * 10000;
        final float uphillmaxslope = rc.expctxWay.getUphillmaxslope() * 10000;
        final float downhillmaxslope = rc.expctxWay.getDownhillmaxslope() * 10000;
        float cfup = rc.expctxWay.getUphillCostfactor();
        float cfdown = rc.expctxWay.getDownhillCostfactor();
        final float cf = rc.expctxWay.getCostfactor();
        cfup = cfup == 0.f ? cf : cfup;
        cfdown = cfdown == 0.f ? cf : cfdown;

        downhillcostdiv = (int) rc.expctxWay.getDownhillcost();
        if (downhillcostdiv > 0) {
            downhillcostdiv = 1000000 / downhillcostdiv;
        }

        int downhillmaxslopecostdiv = (int) rc.expctxWay.getDownhillmaxslopecost();
        if (downhillmaxslopecostdiv > 0) {
            downhillmaxslopecostdiv = 1000000 / downhillmaxslopecostdiv;
        } else {
            // if not given, use legacy behavior
            downhillmaxslopecostdiv = downhillcostdiv;
        }

        uphillcostdiv = (int) rc.expctxWay.getUphillcost();
        if (uphillcostdiv > 0) {
            uphillcostdiv = 1000000 / uphillcostdiv;
        }

        int uphillmaxslopecostdiv = (int) rc.expctxWay.getUphillmaxslopecost();
        if (uphillmaxslopecostdiv > 0) {
            uphillmaxslopecostdiv = 1000000 / uphillmaxslopecostdiv;
        } else {
            // if not given, use legacy behavior
            uphillmaxslopecostdiv = uphillcostdiv;
        }

        final int dist = (int) distance; // legacy arithmetics needs int

        // penalty for turning angle
        final int turncost = (int) ((1. - cosangle) * turncostbase + 0.2); // e.g. turncost=90 -> 90 degree = 90m penalty
        if (message != null) {
            message.linkturncost += turncost;
            message.turnangle = (float) angle;
        }

        double sectionCost = turncost;

        // *** penalty for elevation
        // only the part of the descend that does not fit into the elevation-hysteresis-buffers
        // leads to an immediate penalty

        final int deltaHMicros = (int) (1000000. * deltaH);
        ehbd += -deltaHMicros - dist * downhillcutoff;
        ehbu += deltaHMicros - dist * uphillcutoff;

        float downweight = 0.f;
        if (ehbd > rc.elevationpenaltybuffer) {
            downweight = 1.f;

            int excess = ehbd - rc.elevationpenaltybuffer;
            int reduce = dist * rc.elevationbufferreduce;
            if (reduce > excess) {
                downweight = ((float) excess) / reduce;
                reduce = excess;
            }
            excess = ehbd - rc.elevationmaxbuffer;
            if (reduce < excess) {
                reduce = excess;
            }
            ehbd -= reduce;
            float elevationCost = 0.f;
            if (downhillcostdiv > 0) {
                elevationCost += Math.min(reduce, dist * downhillmaxslope) / downhillcostdiv;
            }
            if (downhillmaxslopecostdiv > 0) {
                elevationCost += Math.max(0, reduce - dist * downhillmaxslope) / downhillmaxslopecostdiv;
            }
            if (elevationCost > 0) {
                sectionCost += elevationCost;
                if (message != null) {
                    message.linkelevationcost += elevationCost;
                }
            }
        } else if (ehbd < 0) {
            ehbd = 0;
        }

        float upweight = 0.f;
        if (ehbu > rc.elevationpenaltybuffer) {
            upweight = 1.f;

            int excess = ehbu - rc.elevationpenaltybuffer;
            int reduce = dist * rc.elevationbufferreduce;
            if (reduce > excess) {
                upweight = ((float) excess) / reduce;
                reduce = excess;
            }
            excess = ehbu - rc.elevationmaxbuffer;
            if (reduce < excess) {
                reduce = excess;
            }
            ehbu -= reduce;
            float elevationCost = 0.f;
            if (uphillcostdiv > 0) {
                elevationCost += Math.min(reduce, dist * uphillmaxslope) / uphillcostdiv;
            }
            if (uphillmaxslopecostdiv > 0) {
                elevationCost += Math.max(0, reduce - dist * uphillmaxslope) / uphillmaxslopecostdiv;
            }
            if (elevationCost > 0) {
                sectionCost += elevationCost;
                if (message != null) {
                    message.linkelevationcost += elevationCost;
                }
            }
        } else if (ehbu < 0) {
            ehbu = 0;
        }

        // get the effective costfactor (slope dependent)
        final float costfactor = cfup * upweight + cf * (1.f - upweight - downweight) + cfdown * downweight;

        if (message != null) {
            message.costfactor = costfactor;
        }

        sectionCost += dist * costfactor + 0.5f;

        return sectionCost;
    }

    @Override
    protected double processTargetNode(final RoutingContext rc) {
        // finally add node-costs for target node
        if (targetNode.nodeDescription != null) {
            final boolean nodeAccessGranted = rc.expctxWay.getNodeAccessGranted() != 0.;
            rc.expctxNode.evaluate(nodeAccessGranted, targetNode.nodeDescription);
            final float initialcost = rc.expctxNode.getInitialcost();
            if (initialcost >= 1000000.) {
                return -1.;
            }
            if (message != null) {
                message.linknodecost += (int) initialcost;
                message.nodeKeyValues = rc.expctxNode.getKeyValueDescription(nodeAccessGranted, targetNode.nodeDescription);
            }
            return initialcost;
        }
        return 0.;
    }

    @Override
    public int elevationCorrection() {
        return (downhillcostdiv > 0 ? ehbd / downhillcostdiv : 0)
                + (uphillcostdiv > 0 ? ehbu / uphillcostdiv : 0);
    }

    @Override
    public boolean definitlyWorseThan(final OsmPath path) {
        final StdPath p = (StdPath) path;

        int c = p.cost;
        if (p.downhillcostdiv > 0) {
            final int delta = p.ehbd / p.downhillcostdiv - (downhillcostdiv > 0 ? ehbd / downhillcostdiv : 0);
            if (delta > 0) {
                c += delta;
            }
        }
        if (p.uphillcostdiv > 0) {
            final int delta = p.ehbu / p.uphillcostdiv - (uphillcostdiv > 0 ? ehbu / uphillcostdiv : 0);
            if (delta > 0) {
                c += delta;
            }
        }

        return cost > c;
    }

    private double calcIncline(final double dist) {
        final double minDelta = 3.;
        double shift = 0.;
        if (elevationBuffer > minDelta) {
            shift = -minDelta;
        } else if (elevationBuffer < -minDelta) {
            shift = minDelta;
        }
        final double decayFactor = Math.exp(-dist / 100.);
        final float newElevationBuffer = (float) ((elevationBuffer + shift) * decayFactor - shift);
        final double incline = (elevationBuffer - newElevationBuffer) / dist;
        elevationBuffer = newElevationBuffer;
        return incline;
    }

    @Override
    protected void computeKinematic(final RoutingContext rc, final double dist, final double deltaH, final boolean detailMode) {
        if (!detailMode) {
            return;
        }

        // compute incline
        elevationBuffer += deltaH;
        final double incline = calcIncline(dist);

        double maxSpeed = rc.maxSpeed;
        final double speedLimit = rc.expctxWay.getMaxspeed() / 3.6f;
        if (speedLimit > 0) {
            maxSpeed = Math.min(maxSpeed, speedLimit);
        }

        double speed = maxSpeed; // Travel speed
        final double fRoll = rc.totalMass * GRAVITY * (rc.defaultCR + incline);
        if (rc.footMode) {
            // Use Tobler's hiking function for walking sections
            speed = rc.maxSpeed * Math.exp(-3.5 * Math.abs(incline + 0.05));
        } else if (rc.bikeMode) {
            speed = solveCubic(rc.sCX, fRoll, rc.bikerPower);
            speed = Math.min(speed, maxSpeed);
        }
        final float dt = (float) (dist / speed);
        totalTime += dt;
        // Calc energy assuming biking (no good model yet for hiking)
        // (Count only positive, negative would mean breaking to enforce maxspeed)
        final double energy = dist * (rc.sCX * speed * speed + fRoll);
        if (energy > 0.) {
            totalEnergy += energy;
        }
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
