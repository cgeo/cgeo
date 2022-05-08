/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.util.FastMathUtils;

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
    }

    @Override
    protected double processWaySection(final RoutingContext rc, final double distance, final double deltaH, final double elevation, final double angle, final double cosangle, final boolean isStartpoint, final int nsection, final int lastpriorityclassifier) {
        // calculate the costfactor inputs
        final float turncostbase = rc.expctxWay.getTurncost();
        float cfup = rc.expctxWay.getUphillCostfactor();
        float cfdown = rc.expctxWay.getDownhillCostfactor();
        final float cf = rc.expctxWay.getCostfactor();
        cfup = cfup == 0.f ? cf : cfup;
        cfdown = cfdown == 0.f ? cf : cfdown;

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
        ehbd += -deltaHMicros - dist * rc.downhillcutoff;
        ehbu += deltaHMicros - dist * rc.uphillcutoff;

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
            if (rc.downhillcostdiv > 0) {
                final int elevationCost = reduce / rc.downhillcostdiv;
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
            if (rc.uphillcostdiv > 0) {
                final int elevationCost = reduce / rc.uphillcostdiv;
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
    public int elevationCorrection(final RoutingContext rc) {
        return (rc.downhillcostdiv > 0 ? ehbd / rc.downhillcostdiv : 0)
                + (rc.uphillcostdiv > 0 ? ehbu / rc.uphillcostdiv : 0);
    }

    @Override
    public boolean definitlyWorseThan(final OsmPath path, final RoutingContext rc) {
        final StdPath p = (StdPath) path;

        int c = p.cost;
        if (rc.downhillcostdiv > 0) {
            final int delta = p.ehbd - ehbd;
            if (delta > 0) {
                c += delta / rc.downhillcostdiv;
            }
        }
        if (rc.uphillcostdiv > 0) {
            final int delta = p.ehbu - ehbu;
            if (delta > 0) {
                c += delta / rc.uphillcostdiv;
            }
        }

        return cost > c;
    }

    private double calcIncline(final double dist) {
        final double minDelta = 3.;
        final double shift;
        if (elevationBuffer > minDelta) {
            shift = -minDelta;
        } else if (elevationBuffer < minDelta) {
            shift = -minDelta;
        } else {
            return 0.;
        }
        final double decayFactor = FastMathUtils.exp(-dist / 100.);
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

        double wayMaxspeed;

        wayMaxspeed = rc.expctxWay.getMaxspeed() / 3.6f;
        if (wayMaxspeed == 0) {
            wayMaxspeed = rc.maxSpeed;
        }
        wayMaxspeed = Math.min(wayMaxspeed, rc.maxSpeed);

        double speed; // Travel speed
        final double fRoll = rc.totalMass * GRAVITY * (rc.defaultCR + incline);
        if (rc.footMode || rc.expctxWay.getCostfactor() > 4.9) {
            // Use Tobler's hiking function for walking sections
            speed = rc.maxSpeed * 3.6;
            speed = (speed * FastMathUtils.exp(-3.5 * Math.abs(incline + 0.05))) / 3.6;
        } else if (rc.bikeMode) {
            speed = solveCubic(rc.sCX, fRoll, rc.bikerPower);
            speed = Math.min(speed, wayMaxspeed);
        } else { // all other
            speed = wayMaxspeed;
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
